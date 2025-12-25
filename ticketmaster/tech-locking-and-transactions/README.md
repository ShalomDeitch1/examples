# Locking + transactions — overview

Ticketmaster must prevent double-booking under heavy contention. This folder compares approaches.

Subprojects:
- `postgres/` — optimistic vs pessimistic locking in Postgres.
- `dynamodb/` — conditional updates + transactional writes.
- `multi-db/` — when state spans multiple stores: Redis locks, sagas, and 2PC.

## Shared invariant

For a given `(eventId, seatId)` there must never be two successful purchases.

## Shared API sketch

- `POST /api/orders` (create order intent)
- `POST /api/orders/{orderId}:confirm` (select seats + authorize payment)
- `POST /api/orders/{orderId}:finalize` (capture + issue ticket)
