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

- `ms1` publica `clients` cuando un cliente se crea o actualiza; `ms2` lo consume y
  actualiza su tabla `client_projection`.
- `ms2` publica `movements` cuando registra un movimiento; `ms1` lo consume y actualiza
  `movement_projection`.

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
