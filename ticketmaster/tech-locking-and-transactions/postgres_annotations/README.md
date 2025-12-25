# Postgres locking (annotations + JPA)

Parallel to [`../postgres/`](../postgres/) but implemented using **Spring annotations** and **Spring Data JPA**.

We model a single table:
`seat_inventory(event_id, seat_id, status, order_id, version)`

## Tech choices
- Spring Boot 3.5.9 (no web server)
- Spring Data JPA + annotations (`@Transactional`, `@Lock`, `@Version`)
- Testcontainers Postgres

## What this shows

1) **Pessimistic locking** using `@Lock(PESSIMISTIC_WRITE)` (row lock / `SELECT .. FOR UPDATE`).

2) **Optimistic locking** using a `version` column (compare-and-swap style update).

## How to run

```bash
mvn test
```
