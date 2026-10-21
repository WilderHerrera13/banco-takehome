# Comentarios para el ejercicio (CoderPad)

Texto a pegar en los comentarios del ejercicio. Reemplazar la URL de ejemplo por la real
del repositorio antes de enviarlo.

---

**Solución entregada — Arquitectura de microservicios (Java / Spring Boot)**

Repositorio: https://github.com/<TU-USUARIO>/banco-takehome
ZIP adjunto: `banco-takehome.zip` (descomprimir y `docker compose up --build`).

**Cómo se ejecuta**

```bash
unzip banco-takehome.zip -d banco-takehome
cd banco-takehome
docker compose up --build
```

Levanta PostgreSQL (crea el esquema y carga los datos de `BaseDatos.sql` solo),
Apache Kafka en modo KRaft y los dos microservicios: `ms1-person-client` (8081) y
`ms2-account-movement` (8082). Verificación funcional de todo el flujo con
`./verificar.sh`.

**Qué contiene**

- `ms1-person-client`: `Person` (`@MappedSuperclass`) y `Client` (CRUD completo en
  `/clientes`).
- `ms2-account-movement`: `Account` y `Movement` (CRU en `/cuentas` y `/movimientos`)
  y el reporte de estado de cuenta en `/reportes`.
- `shared`: contrato compartido entre los dos servicios (DTOs, eventos y errores).
- Comunicación asíncrona bidireccional por Kafka: ms1 publica `clients` y ms2 publica
  `movements`; cada servicio mantiene una proyección local de lo que recibe.
- `BaseDatos.sql`, colección de Postman, Dockerfiles por servicio, `docker-compose.yml`,
  `README.md` con las instrucciones y `verificar.sh` con las comprobaciones funcionales.

**Requisitos y dónde están**

| Requisito | Dónde | Cómo se comprueba |
|---|---|---|
| F1 CRUD de clientes | `ClientController` | `POST/GET/PUT/DELETE /clientes` |
| F1 CRU de cuentas y movimientos | `AccountController`, `MovementController` | `POST/GET/PUT /cuentas` y `/movimientos` |
| F2 Registro de movimientos | `MovementService.register` | el saldo de la cuenta se actualiza y la transacción queda registrada con su saldo |
| F3 Saldo no disponible | `InsufficientFundsException` + `ApiExceptionHandler` | `HTTP 422` con el mensaje `Saldo no disponible` y el detalle de cuenta, saldo disponible y valor solicitado |
| F4 Reporte estado de cuenta | `AccountStatementService` | `GET /reportes?fecha=desde..hasta&cliente=id` devuelve JSON con cuentas y movimientos |
| F5 Prueba unitaria de cliente | `ClientServiceTest` | `mvn test` |
| F6 Prueba de integración | `ClientIntegrationTest`, `AccountStatementIntegrationTest` | `mvn test` |
| F7 Docker | `Dockerfile` por servicio, `docker-compose.yml` | `docker compose up --build` |

**Decisiones que vale la pena discutir**

- Base de datos por servicio: cada microservicio es dueño de su información y se comunica
  con eventos, no con llamadas HTTP síncronas; el reporte responde con su proyección local.
- Consistencia del saldo: bloqueo pesimista (`@Lock(PESSIMISTIC_WRITE)`) sobre la cuenta
  dentro de la misma transacción del movimiento, más control de versión optimista, para que
  dos retiros simultáneos no puedan descontar más de lo que hay.
- Errores: 400 para datos inválidos, 404 para recursos inexistentes, 409 para duplicados y
  422 para el saldo insuficiente, que es una regla de negocio y no un error de la petición.
- Mapeo entidad-DTO con MapStruct: el mapeo se genera en tiempo de compilación, así que un
  cambio en el dominio rompe el build en lugar de devolver un JSON incompleto en silencio.
