# Arquitectura de Microservicios — Client/Person y Account/Movement

Solución de la prueba técnica Backend BP: dos microservicios Spring Boot con comunicación
asíncrona por eventos, persistencia JPA sobre PostgreSQL, manejo de excepciones, pruebas
unitarias y de integración, y despliegue en Docker.

## 0. Cómo se ejecuta y valida (lo primero)

```bash
unzip banco-takehome.zip -d banco-takehome
cd banco-takehome
docker compose up --build          # Postgres + Kafka + los 2 microservicios
```

Cuando levante, verificar en otra terminal:

```bash
./verificar.sh                     # 40+ comprobaciones funcionales, una por requisito
```

O a mano:

```bash
curl http://localhost:8081/clientes
curl -X POST http://localhost:8082/movimientos/225487 -H 'Content-Type: application/json' \
     -d '{"date":"2022-02-25","movementType":"DEPOSIT","amount":600.00}'
curl "http://localhost:8082/reportes?fecha=2022-02-01..2022-02-28&cliente=cli-marianela-montalvo"
```

`docker compose down -v` borra también los volúmenes y devuelve el stack al estado inicial
(el esquema y los datos de los casos de uso se crean solos al arrancar PostgreSQL).

> El código (paquetes, clases, métodos, tablas) está en inglés. Las **rutas de la API** y el
> mensaje de error de saldo se mantienen tal como los exige el enunciado (`/clientes`,
> `/cuentas`, `/movimientos`, `/reportes` y `"Saldo no disponible"`).

## 1. Arquitectura

```
                    +--------------------------+
                    |  PostgreSQL              |
                    |  banco_ms1 / banco_ms2   |
                    +-----------+--------------+
                                |
        +-----------------------+-----------------------+
        |                                               |
+-------v---------+                           +---------v--------+
| ms1-person-     |   evento: clients         | ms2-account-     |
| client   (8081) |  ------------------------>  | movement  (8082) |
|                 |                             |                  |
| Person          | <-------------------------  | Account          |
| Client          |   evento: movements        | Movement         |
| (CRUD)          |                             | (CRU)            |
|                 |                             | Report (F4)      |
+-----------------+                             +------------------+

              ms2 mantiene la proyeccion local de clients
              ms1 mantiene la proyeccion local de movements
```

Comunicación asíncrona con Apache Kafka (temas `clients` y `movements`). Cada servicio es
dueño de su base de datos (*database per service*) y no consulta la base del otro: se
comunica con eventos de dominio y mantiene una proyección local.

| Servicio | Puerto | Responsabilidad | Endpoints |
|---|---|---|---|
| `ms1-person-client` | 8081 | Person, Client, CRUD completo | `/clientes` |
| `ms2-account-movement` | 8082 | Account, Movement, Report | `/cuentas`, `/movimientos`, `/reportes` |

### Modelo de dominio

- `Person` (`@MappedSuperclass`): name, gender, age, identification, address, phone.
- `Client extends Person`: `client_id` (PK), password, active. Identificación única.
- `Account`: `account_number` (PK), accountType, initialBalance, currentBalance, active, `client_id`.
- `Movement`: id, date, movementType, amount (con signo), balance (saldo tras el movimiento), account.
- Proyecciones locales: `client_projection` (en ms2) y `movement_projection` (en ms1).

## 2. Puesta en marcha con Docker

```bash
docker compose up --build
```

Levanta PostgreSQL (con el esquema y los datos de `BaseDatos.sql`), Kafka en modo KRaft y los
dos microservicios. Comprobación:

```bash
curl http://localhost:8081/clientes
curl "http://localhost:8082/cuentas?clienteId=cli-marianela-montalvo"
curl "http://localhost:8082/reportes?fecha=2022-02-01..2022-02-28&cliente=cli-marianela-montalvo"
```

## 3. Puesta en marcha local

```bash
mvn clean package                 # compila y ejecuta las pruebas
docker compose up -d postgres kafka
mvn -pl ms1-person-client spring-boot:run     # http://localhost:8081
mvn -pl ms2-account-movement spring-boot:run   # http://localhost:8082
```

Variables de entorno: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP`.

## 4. Funcionalidades

| Requisito | Implementación | Verificación |
|---|---|---|
| F1 CRUD/CRU | `ClientController` (POST/GET/PUT/DELETE `/clientes`), `AccountController` y `MovementController` (POST/GET/PUT) | Colección Postman |
| F2 Registro de movimientos | `MovementService.register`: el amount toma el signo según el movementType, actualiza el saldo de la cuenta y guarda la transacción con su balance resultante | `MovementServiceTest`, `AccountStatementIntegrationTest` |
| F3 Saldo no disponible | `InsufficientFundsException` cuando el saldo resultante es negativo; `ApiExceptionHandler` responde 422 con el mensaje `Saldo no disponible` y el detalle (cuenta, saldo disponible, valor solicitado) | `MovementServiceTest` |
| F4 Reporte estado de cuenta | `AccountStatementController` → `/reportes?fecha=desde..hasta&cliente=id` devuelve JSON con las cuentas (saldo inicial y actual) y los movimientos del periodo | `AccountStatementIntegrationTest` |
| F5 Prueba unitaria de Client | `ClientServiceTest` (4 casos: alta, duplicado, lectura y ausencia) | `mvn test` |
| F6 Prueba de integración | `ClientIntegrationTest` y `AccountStatementIntegrationTest`: contexto de Spring completo, HTTP real, base de datos en memoria (H2) y Kafka embebido | `mvn test` |
| F7 Docker | `Dockerfile` multi-stage por servicio y `docker-compose.yml` | `docker compose up --build` |

## 5. Decisiones de diseño

**Asincronía.** El servicio de clientes publica `clients` y el de cuentas lo consume para
mantener `client_projection`; a la inversa, el de cuentas publica `movements` y el de
clientes proyecta. Así el reporte (F4) responde con datos propios sin llamar al otro
servicio en línea, y el dominio de cada servicio sigue siendo dueño de su información.

**Consistencia del saldo.** El registro de un movimiento toma un bloqueo pesimista
(`@Lock(PESSIMISTIC_WRITE)`) sobre la cuenta: dos retiros simultáneos no pueden descontar
más de lo que hay. El saldo de cada movimiento es una foto del saldo en ese instante, y al
actualizar un movimiento se recalcula el saldo de la cuenta y de los movimientos posteriores.

**Errores.** Cada microservicio tiene su `@RestControllerAdvice` con una respuesta común
(`ErrorResponse`): 400 para datos inválidos, 404 para recursos inexistentes, 409 para
duplicados y 422 para el saldo insuficiente, que es un error de regla de negocio y no de
sintaxis de la petición.

**Capas.** Controller (HTTP) → Service (reglas de negocio) → Repository (Spring Data).
Los DTOs de entrada y salida son distintos de las entidades, y el contrato entre servicios
(formato JSON de los eventos) vive en el módulo `shared`.

## 6. Pruebas

```bash
mvn test
```

- `ClientServiceTest` — prueba unitaria con Mockito (F5).
- `ClientIntegrationTest` — contexto de Spring, HTTP real contra el endpoint y Kafka embebido.
- `MovementServiceTest` — regla de saldo y de insuficiencia de fondos.
- `AccountStatementIntegrationTest` — movimiento + reporte de estado de cuenta.

Las pruebas de integración no dependen de Docker: la base de datos es H2 en memoria
(perfil `test`) y el broker es el de Kafka embebido, de modo que `mvn test` corre en
cualquier máquina sin infraestructura externa. El esquema de producción es el de
`BaseDatos.sql` sobre PostgreSQL y se valida con `ddl-auto: validate` al arrancar.

## 7. Entregables

- `BaseDatos.sql` — creación de las dos bases, tablas, índices y datos de los casos de uso.
- `verificar.sh` — comprobación funcional de todos los endpoints (F1–F4) y del asíncrono.
- `postman/BancoAPI.postman_collection.json` — colección para validar los endpoints.
- `docker-compose.yml` y los `Dockerfile` de cada servicio.
- `GUIA-DE-ENTRENAMIENTO.md` — guion para defender la solución en la entrevista.
- Este `README.md` con las instrucciones de despliegue.
