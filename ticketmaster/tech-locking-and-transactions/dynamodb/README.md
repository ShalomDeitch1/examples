# DynamoDB locking (conditional writes + transactions)

Smallest possible demo of **how DynamoDB prevents double-booking**.

We use DynamoDB Local in tests and two tables:
- `seat_inventory(seat_id, status, order_id)`
- `orders(order_id, seat_id)`

## Tech choices
- Spring Boot 3.5.9 (no web server)
- AWS SDK v2 (DynamoDB)
- Testcontainers (DynamoDB Local)

## How to run

```bash
mvn test
```

## Conditional write

Guarantee: only one caller can transition `status` from `AVAILABLE` → `RESERVED`.

```mermaid
flowchart TD
  A[UpdateItem with ConditionExpression] --> B{condition true?}
  B -->|yes| C[Seat RESERVED]
  B -->|no| D[ConditionalCheckFailed]
```

## Transactional write

Guarantee: reserve seat + create order happen together.

```mermaid
sequenceDiagram
  participant App as App
  participant DDB as DynamoDB

  App->>DDB: TransactWriteItems
  Note over DDB: 1) Update seat if AVAILABLE
  Note over DDB: 2) Put order if not exists
  DDB-->>App: success OR conditional failure
```
