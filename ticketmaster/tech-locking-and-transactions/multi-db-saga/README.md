# Multi-DB (Saga)

Smallest possible demo of a **saga** when one business operation spans **two databases**.

We model a purchase as:
1) Reserve seat (Inventory DB)
2) Authorize payment (Payments DB)
3) Confirm seat (Inventory DB)

If payment fails, we run **compensation**: release the seat reservation.

## Tech choices
- Spring Boot 3.5.9 (no web server)
- Plain JDBC (no Flyway; schema created in tests)
- Testcontainers Postgres (two containers: inventory + payments)

## How to run

```bash
mvn test
```

## Saga flow


# Multi-DB (Saga)

Smallest possible demo of a **saga** when one business operation spans **two databases**.

We model a purchase as:
1) Reserve seat (Inventory DB)
2) Authorize payment (Payments DB)
3) Confirm seat (Inventory DB)

If payment fails, we run **compensation**: release the seat reservation.

## Tech choices
- Spring Boot 3.5.9 (no web server)
- Plain JDBC (no Flyway; schema created in tests)
- Testcontainers Postgres (two containers: inventory + payments)

## How to run

```bash
mvn test
```

## Saga flow

```mermaid
sequenceDiagram
  participant O as Orchestrator
  participant I as Inventory DB
  participant P as Payments DB

  O->>I: reserve(seatId, sagaId)
  alt reserved
    O->>P: authorize(sagaId, amount)
    alt authorized
      O->>I: confirm(seatId, sagaId)
    else declined
      O->>I: release(seatId, sagaId)  %% compensation
    end
  else seat not available
    O-->>O: stop
  end
```

