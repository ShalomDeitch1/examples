# Postgres locking (pessimistic + optimistic)

Simple demo of **how Postgres prevents double-booking**.

We model a single table `seat_inventory(event_id, seat_id, status, version)`.

## Tech choices
- Spring Boot 3.5.9 (no web server)
- Spring JDBC transactions
- Testcontainers Postgres (tests start Docker automatically)

## What this shows

1) **Pessimistic locking**: `SELECT ... FOR UPDATE` (one thread holds the row lock).

2) **Optimistic locking**: `UPDATE ... WHERE version = ?` (only one update wins).

## How to run

```bash
mvn test
```

## Pessimistic locking (row lock)

```mermaid
sequenceDiagram
  participant T1 as Thread 1
  participant T2 as Thread 2
  participant PG as Postgres

  T1->>PG: BEGIN
  T1->>PG: SELECT seat FOR UPDATE
  T2->>PG: BEGIN
  T2->>PG: SELECT seat FOR UPDATE
  Note over T2,PG: waits for T1 to COMMIT
  T1->>PG: UPDATE status=RESERVED
  T1->>PG: COMMIT
  T2->>PG: sees RESERVED
  T2->>PG: ROLLBACK
```

## Optimistic locking (version column)

```mermaid
flowchart TD
  A[Read seat: version=v] --> B[Try UPDATE ... WHERE version=v]
  B --> C{rows updated == 1?}
  C -->|yes| D[Success]
  C -->|no| E[Someone else won]
```

## Trade-offs
- Pessimistic: easiest to reason about, but can block under contention.
- Optimistic: no blocking, but requires retry/"someone won" handling.
