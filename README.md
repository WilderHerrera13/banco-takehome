# Arquitectura de microservicios — Client/Person y Account/Movement

Dos microservicios Spring Boot (Java 17) que resuelven el dominio de clientes y cuentas
bancarias, con comunicación asíncrona entre ellos.

## Cómo se ejecuta

```bash
docker compose up --build
```

Levanta PostgreSQL (crea el esquema y carga los datos iniciales de `BaseDatos.sql`),
Apache Kafka y los dos microservicios:

| Servicio | Puerto | Qué expone |
|---|---|---|
| `ms1-person-client` | 8081 | `/clientes` |
| `ms2-account-movement` | 8082 | `/cuentas`, `/movimientos`, `/reportes` |

Para dejarlo como estaba: `docker compose down -v`.

## Estructura

```
shared/                  contrato común entre los dos servicios (DTOs, eventos, errores)
ms1-person-client/       Person (@MappedSuperclass) y Client
ms2-account-movement/    Account, Movement y el reporte de estado de cuenta
```

Cada servicio tiene su propia base de datos y su propio esquema. No se consultan entre
ellos: se comunican con eventos de Kafka y cada uno guarda una proyección local de lo que
recibe.

- `ms1` publica `clients` cuando un cliente se crea o actualiza, y `clients-deleted` cuando
  se borra; `ms2` los consume y mantiene su tabla `client_projection` al día.
- `ms2` publica `movements` cuando registra o actualiza un movimiento; `ms1` lo consume y
  actualiza `movement_projection`.

## API

```bash
# clientes
curl http://localhost:8081/clientes
curl -X POST http://localhost:8081/clientes -H 'Content-Type: application/json' \
  -d '{"name":"Ana Torres","gender":"FEMALE","age":29,"identification":"1712345701",
       "address":"Calle 100 # 10-20","phone":"3112223344","password":"clave123","active":true}'

# cuentas
curl -X POST http://localhost:8082/cuentas -H 'Content-Type: application/json' \
  -d '{"accountNumber":"778899","accountType":"SAVINGS","initialBalance":1000.00,
       "active":true,"clientId":"cli-jose-lema"}'

# movimientos
curl -X POST http://localhost:8082/movimientos/225487 -H 'Content-Type: application/json' \
  -d '{"date":"2022-02-25","movementType":"DEPOSIT","amount":600.00}'

# reporte de estado de cuenta
curl "http://localhost:8082/reportes?fecha=2022-02-01..2022-02-28&cliente=cli-marianela-montalvo"
```

La colección de Postman con todos los endpoints está en `postman/`.

## Reglas de negocio

- Un movimiento tiene valor positivo o negativo según el tipo (`DEPOSIT` / `WITHDRAWAL`) y
  actualiza el saldo disponible de la cuenta.
- Si al aplicar un movimiento el saldo quedaría en negativo, la operación se rechaza con
  `HTTP 422` y el mensaje `Saldo no disponible`, junto con el detalle de la cuenta, el
  saldo disponible y el valor solicitado. El saldo no se modifica.
- Al registrar un movimiento se guarda la transacción con el saldo que había después de
  aplicarlo, para poder reconstruir el estado de cuenta.
- El reporte devuelve las cuentas del cliente con su saldo inicial y actual, y los
  movimientos del rango pedido, en JSON.

Los errores llegan siempre en el mismo formato (`timestamp`, `status`, `error`, `message`,
`path`, `details`): `400` para datos inválidos, `404` si no existe el recurso, `409` para
duplicados y `422` cuando la operación es válida pero el saldo no alcanza.

## Rendimiento, escalabilidad y resiliencia

La solución está planteada para crecer y para no depender de la disponibilidad de otro
servicio, aunque en este ejercicio no se implemente todavia:

- **Rendimiento**: el saldo se calcula y se guarda, no se recalcula en cada lectura (cada
  movimiento guarda el saldo que dejó la cuenta). El lock pesimista se aplica solo a la
  cuenta que se toca, no a toda la base. Los indices de `movement (account_number, date)` y
  `account (client_id)` cubren las consultas del reporte y de los listados por cliente.
- **Escalabilidad**: cada microservicio tiene su base y su topic de Kafka, asi que se pueden
  escalar por separado. La comunicacion asincrona permite que `ms2` procese los eventos a
  su ritmo sin que `ms1` espere, y las proyecciones son idempotentes (clave primaria), de
  modo que un consumidor puede releer un topic sin duplicar nada.
- **Resiliencia**: un fallo al publicar un evento no rompe la transaccion de negocio (el
  resultado del `send` se registra en el log para poder reintentar), y los consumidores
  toleran eventos repetidos. En produccion el siguiente paso seria un *outbox*
  transaccional y una cola de reintentos con *dead letter* para los mensajes que no se
  puedan procesar.

## Pruebas

```bash
mvn test
```

- `ClientServiceTest`: prueba unitaria del servicio de clientes.
- `MovementServiceTest`: regla de saldo y de insuficiencia de fondos.
- `ClientIntegrationTest` y `AccountStatementIntegrationTest`: pruebas de integración con
  el contexto de Spring, HTTP real, base de datos en memoria y Kafka embebido.

## Entregables

- `BaseDatos.sql`: creación de las dos bases, tablas, índices y datos de los casos de uso.
- `postman/`: colección para validar los endpoints.
- `Dockerfile` por servicio y `docker-compose.yml` para levantar todo.
