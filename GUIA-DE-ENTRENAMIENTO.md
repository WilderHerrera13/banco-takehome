# Guía de entrenamiento — defender esta solución en la entrevista

Objetivo: que puedas explicar **cada decisión** del take-home con tus propias palabras y
responder las preguntas que te van a hacer. No es un resumen del código: es el guion de
"por qué lo hiciste así".

---

## 1. El enunciado en una frase

> Dos microservicios Spring Boot con comunicación asíncrona: uno de Person/Client y otro de
> Account/Movement, con JPA, manejo de excepciones, pruebas y despliegue en Docker.

Y el detalle que más importa: **esto no es un CRUD con dos tablas**. Piden tres cosas que
delatan a un buen ingeniero:

1. Los dos servicios **no se hablan por HTTP de forma síncrona** (eso sería un monolito
   disfrazado). Se comunican por **eventos**.
2. **Cada servicio es dueño de su base de datos**. No hay una tabla compartida.
3. El **saldo de una cuenta no puede quedar desincronizado** con sus movimientos, ni aunque
   lleguen dos retiros a la vez.

Si en la entrevista explicas solo los endpoints, se nota el trabajo. Si explicas estas tres
decisiones, se nota la arquitectura.

> Nota de estilo: el código está en inglés (clases, paquetes, tablas) pero **las rutas de la
> API y el mensaje `"Saldo no disponible"` se dejan tal como los pide el enunciado**. Si te
> preguntan por eso, la respuesta es: "las rutas son contrato con el evaluador; el nombre
> del código es el de mi criterio".

---

## 2. Mapa del proyecto (memorízalo)

```
banco-takehome/
├── pom.xml                        padre: Spring Boot 3.3.5, Java 17, 3 módulos
├── shared/                        contrato común: DTOs, eventos, errores
│   └── com/bp/shared/{dto,event,exception}
├── ms1-person-client/            puerto 8081
│   └── com/bp/client/
│       ├── models/         Person (@MappedSuperclass), Client, Gender, MovementProjection
│       ├── repository/     ClientRepository, MovementProjectionRepository
│       ├── service/        ClientService, ClientEventPublisher
│       ├── messaging/      MovementRegisteredConsumer
│       ├── controllers/    ClientController, ApiExceptionHandler
│       └── exception/      ClientNotFoundException, DuplicateIdentificationException
├── ms2-account-movement/         puerto 8082
│   └── com/bp/account/
│       ├── models/         Account, Movement, AccountType, MovementType, ClientProjection
│       ├── repository/     AccountRepository, MovementRepository, ClientProjectionRepository
│       ├── service/        AccountService, MovementService, AccountStatementService, MovementEventPublisher
│       ├── messaging/      ClientRegisteredConsumer
│       ├── controllers/    AccountController, MovementController, AccountStatementController, ApiExceptionHandler
│       └── exception/      AccountNotFoundException, UnknownClientException
├── BaseDatos.sql                  esquema + datos de los casos de uso
├── docker-compose.yml             postgres + kafka + los 2 servicios
├── postman/                       colección para validar los endpoints
└── GUIA-DE-ENTRENAMIENTO.md       este documento
```

Tres módulos Maven y no un solo proyecto: `shared` es una biblioteca normal (no arranca
nada) con el **contrato** entre los dos servicios.

---

## 3. Las seis decisiones que debes saber explicar

### 3.1 Por qué eventos y no llamadas HTTP entre servicios

Porque el requisito dice "comunicación asíncrona", y porque es lo correcto cuando los dos
dominios no dependen el uno del otro para leer.

- `ms1` publica el evento `clients` cuando un cliente se crea o actualiza.
- `ms2` lo consume y guarda una **proyección local** en `client_projection`.
- `ms2` publica `movements` cuando registra un movimiento.
- `ms1` lo consume y guarda su proyección en `movement_projection`.

Consecuencia práctica: el **reporte (F4) responde sin llamar a nadie**. Si `ms1` está caído,
el reporte de ms2 sigue funcionando con la proyección local. Ese es el argumento de
"desacoplamiento" y es lo que hay que decir si te preguntan "¿y si el otro servicio no
responde?".

**Lo que NO es**: no es "el otro microservicio no sabe nada del mío" sin más. La
proyección es una copia local deliberada, con su propia tabla, su propio esquema y su
propio ciclo de vida. Si mañana el evento cambia de forma, se actualiza la proyección, no
el consumidor.

**Kafka y no una cola en memoria**: el evento sobrevive a una caída del consumidor (el offset
queda en el topic), se puede reprocesar y varios consumidores pueden leer el mismo evento.
En modo KRaft (sin Zookeeper) por simplicidad de despliegue.

### 3.2 Por qué `Person` es `@MappedSuperclass` y no una tabla propia

El enunciado dice "Client debe manejar una entidad, **que herede de la clase person**".
Con `@MappedSuperclass` el mapeo JPA se hereda, pero la tabla física es una sola (`client`)
con las columnas de person y de client. Sin esto habrías creado una tabla `person` con una
PK y una FK `client_id` en `client`: eso es una relación 1–1, no herencia, y obligaría a
hacer JOIN en cada lectura. Explica el coste: `person` sin clave propia no puede existir
sola en el modelo relacional.

### 3.3 El saldo: por qué hay `initial_balance` **y** `current_balance`

- `initial_balance` es el dato de partida (el del caso de uso) y **no se toca**.
- `current_balance` es el saldo vigente = inicial + Σ movimientos.

Mantener el inicial es lo que permite el reporte "estado de cuenta" tal como lo pide el
caso de uso (el ejemplo del enunciado muestra `Saldo Inicial` y `Saldo Disponible` por
separado).

**El movimiento guarda el balance que había después de aplicarlo**, que es una foto en el
tiempo: por eso el reporte puede mostrar el saldo de cada movimiento y no solo el final.

### 3.4 La concurrencia: `@Lock(PESSIMISTIC_WRITE)`

Dos retiros simultáneos sobre la misma cuenta con saldo 100 y 50:
- Sin bloqueo: ambos leen 100, ambos escriben 50 → **se pierde dinero**.
- Con `PESSIMISTIC_WRITE` sobre la cuenta: la segunda transacción **espera** a que la
  primera termine, y entonces ve el saldo ya descontado → rechaza el retiro si no alcanza.

Es un `SELECT ... FOR UPDATE` implícito, y va **dentro de la misma transacción** que el
update del saldo. Si el lock estuviera fuera de la transacción no serviría de nada.

Además la entidad `Account` tiene `@Version` (lock optimista) como segunda barrera: si dos
transacciones tocan la misma fila, la segunda falla y Spring la reintenta.

### 3.5 Los errores: por qué 422 y no 400

`InsufficientFundsException` es una **regla de negocio**, no un error de sintaxis de la
petición: la petición estaba bien formada, el servidor la entiende y la rechaza por el
estado del dominio. Por eso:

- **400** → la petición está mal (validación de campos, fechas mal formadas).
- **404** → el recurso no existe (cliente, cuenta).
- **409** → conflicto (identificación duplicada, cuenta ya existente).
- **422** → la petición es válida pero el dominio la rechaza (saldo insuficiente).

El cuerpo es siempre el mismo (`ErrorResponse`): timestamp, status, error, message, path y
un mapa `details` con el contexto del fallo (cuenta, saldo disponible, valor solicitado).
En un sistema real, ese 422 llevaría además un código de negocio estable
(`SALDO_NO_DISPONIBLE`) para que el cliente lo pueda tratar sin parsear el texto.

### 3.7 Por qué MapStruct y no mapeo a mano

El mapeo entidad <-> DTO es boilerplate: los mismos campos, uno por uno, y cada vez que
añades un campo al dominio tienes que acordarte de tocar el mapper (y recordarlo).

Con **MapStruct** (procesador de anotaciones):

```java
@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDto toDto(Account account);

    @Mapping(target = "currentBalance", source = "initialBalance")
    Account toEntity(AccountDto dto);

    default AccountType toAccountType(String value) {
        return AccountType.valueOf(value.toUpperCase());
    }
}
```

Ventajas frente a escribirlo a mano:

- **Se ejecuta en tiempo de compilación**: si un campo cambia de nombre o de tipo, el build
  falla, no la producción. El mapeo a mano se rompe en silencio.
- **Cero código generado a mano**: el mapper generado es una clase que el IDE nunca Teach
  -- ni ves, ni tocas.
- **Un unico lugar por entidad**: cambiar el dominio obliga a tocar el mapper, porque el
  compilador te avisa.
- **Conversiones de tipo**: el metodo `default` resuelve `String -> enum` sin ifs.
- **Cero reflexion en runtime**: el codigo generado es Java plano.

Lo que hay que saber si te lo preguntan:

- Es una dependencia mas (`mapstruct` + `mapstruct-processor` como `annotationProcessorPath`),
  asi que el build necesita el processor (Maven ya lo resuelve).
- Para entidades con campos que no vienen del DTO (por ejemplo `currentBalance`, que se
  calcula a partir de `initialBalance`) se declara con `@Mapping` explicito: no adivina.
- Con `componentModel = "spring"` el mapper es un bean y se inyecta en el servicio.
- Un mapper con many campos y reglas de negocio no es un mapper: en ese caso va en el
  servicio, no en el mapper.

### 3.6 Capas y por qué los DTOs son distintos de las entidades

Controller (HTTP) → Service (reglas) → Repository (datos). El Service no sabe de HTTP y el
Controller no tiene reglas de negocio.

Los DTOs de entrada (`ClientRequest`, `AccountRequest`, `MovementRequest`) están
separados de la entidad **para no exponer la contraseña** en las respuestas: `ClientDto`
sí la incluye porque es el contrato de entrada, pero `GET /clientes` también la devuelve
hoy — y eso es un defecto que debes reconocer si te lo señalan (en producción la
contraseña iría hasheada y la respuesta la omitiría).

---

## 4. Recorrido guiado del código (en este orden)

1. `ms1/.../models/Person.java` y `Client.java` — el modelo y la herencia.
2. `ms1/.../service/ClientService.java` — CRUD + publicación del evento.
3. `ms1/.../messaging/MovementRegisteredConsumer.java` — el otro sentido del asíncrono.
4. `ms2/.../models/Account.java` y `Movement.java` — el saldo y la foto.
5. `ms2/.../repository/AccountRepository.java` — el `findByAccountNumberForUpdate` (el lock).
6. `ms2/.../service/MovementService.java` — **la pieza clave**: el registro, el saldo y el error.
7. `ms2/.../service/AccountStatementService.java` + `controllers/AccountStatementController.java` — F4.
8. `ms2/.../controllers/ApiExceptionHandler.java` — el manejo de errores.
9. `BaseDatos.sql` — el esquema y los datos de los casos de uso.

---

## 5. Preguntas que te van a hacer (y qué responder)

**"¿Por qué dos microservicios y no un monolito modular?"**
Porque los dos dominios cambian a ritmos distintos y tienen datos distintos; además el
requisito lo pide. Con un monolito, el cliente y la cuenta quedarían en el mismo esquema y
un fallo o un despliegue de uno afectaría al otro. En un monolito modular, además, el
reporte acabaría consultando dos contextos en la misma transacción.

**"¿Qué pasa si el evento se duplica?"**
Hoy el consumidor hace `save` por id de movimiento, así que reprocesar el mismo evento es
idempotente. En un sistema real lo que se garantiza es **idempotencia en el consumidor**:
la clave de idempotencia (id del movimiento) se persiste en la misma transacción que el
efecto, de modo que reprocesar un evento ya aplicado no duplica el saldo.

**"¿Cómo garantizas que el saldo y los movimientos no se desincronicen?"**
Con la transacción: el movimiento, el saldo de la cuenta y el evento salen juntos (el
outbox sería el paso siguiente: guardar el evento en la misma tabla/outbox y publicarlo
con un relay, en lugar de publicarlo dentro de la transacción, para que no haya un hueco
entre "guardé el movimiento" y "publicé el evento").

**"¿Por qué Kafka y no RabbitMQ o HTTP asíncrono?"**
Kafka me da retención, reprocesamiento y varios consumidores por evento; el outbox
pattern encaja mejor con una cola de trabajos. HTTP asíncrono no me da nada de eso.

**"¿Cómo lo monitorizarías?"**
Métricas RED (tasa de peticiones, tasa de errores, duración) por endpoint, más el retraso
del consumidor de Kafka (lag del topic `movements`) y una métrica de negocio: saldo
total por cuenta vs. suma de movimientos, que es exactamente el invariante que el
reporte de F4 permite verificar.

**"¿Por qué MapStruct y no el mapper a mano?"**
Porque el mapeo es boilerplate que se rompe en silencio: si mañana `Account` gana un campo,
el `toDto` a mano sigue compilando y devuelve un JSON incompleto. MapStruct lo genera en
tiempo de compilacion, asi que el fallo aparece en el build. Ademas quita codigo en vez de
anadirlo, y las conversiones de tipo (String -> enum) quedan en un metodo `default` en vez
de un if en el servicio.

**"¿Qué mejorarías si tuvieras más tiempo?"**
1. Outbox + relay en lugar de publicar dentro de la transacción.
2. Idempotencia explícita en el consumidor con clave de negocio.
3. `traceId` en las respuestas y MDC para correlación entre servicios.
4. OpenAPI/Swagger y contract tests entre los dos servicios.
5. Migraciones con Flyway en vez de `ddl-auto: validate` + script manual.

---

## 6. Ejercicios para que lo interiorices (en este orden)

1. **Explica en voz alta** el recorrido de una petición: `POST /movimientos/225487` → qué
   clases se tocan, en qué orden, y qué pasa en la base de datos.
2. **Dibuja el diagrama** de los dos servicios con el broker en el medio y las dos
   proyecciones; si puedes dibujarlo sin mirar el código, lo sabes.
3. **Predice** qué devuelve `GET /reportes` si el evento `clients` aún no ha llegado a ms2
   (respuesta: 404 "The client ... is not known by the client service"; es el precio de la
   asincronía y hay que decirlo).
4. **Ejecuta** el proyecto y rompe algo a propósito: quita el lock y lanza dos retiros
   simultáneos para ver el saldo descuadrado; luego devuélvelo. Ver el bug es lo que
   hace que la explicación sea creíble.
5. **Cierra y vuelve a abrir**: `docker compose down -v && docker compose up --build`, y
   repite el flujo de Postman.

---

## 7. Cómo presentar el resultado

- README arriba: qué es, cómo se levanta (`docker compose up --build`), endpoints y
  requisitos mapeados.
- Diagrama de arquitectura con el broker y las dos proyecciones.
- La tabla de requisitos (F1–F7) con el endpoint y la prueba que lo cubre.
- Menciona la interview que sigue: "lo que subo es la solución y además puedo defender
  cada decisión".

Ese último punto es la diferencia entre "mandé un ZIP" y "entregué un take-home".
