# Locking + Transactions

This directory compares approaches for preventing double-booking under high contention.

## Key concepts
- Invariant: for a given `(eventId, seatId)` only one transition to `SOLD` may occur.
- Prefer single-store atomic operations where possible; use distributed coordination or sagas when state spans stores.
- Use idempotency for mutation endpoints to make retries safe.

## Subprojects (why each exists)
- [postgres/](./postgres/) — demonstrates optimistic vs. pessimistic locking and transaction patterns to protect a single relational store.
- [postgres_annotations/](./postgres_annotations/) — same Postgres demo, but implemented using Spring annotations + Spring Data JPA (`@Transactional`, `@Lock`, `@Version`).
- [dynamodb/](./dynamodb/) — shows conditional updates and `TransactWriteItems` for multi-item consistency in DynamoDB.
- [multi-db-redis/](./multi-db-redis/) — illustrates using a Redis-based distributed lock (lease + token-checked release) to coordinate access across services/datastores.
- [multi-db-saga/](./multi-db-saga/) — implements a saga with compensation when an operation must span multiple databases and strict atomicity is impossible.


