# Plan — Postgres locking

## Goal
Define and later implement seat reservation and purchase using Postgres transactions with both pessimistic and optimistic locking.

## TODO
- [ ] Define schema for `seat_inventory` with `version` and `reserved_until`.
- [ ] Implement pessimistic reserve path (`SELECT FOR UPDATE`).
- [ ] Implement optimistic reserve path (version checks) + retry policy.
- [ ] Implement finalize path (capture + mark SOLD).
- [ ] Unit tests for concurrency logic (simulated contention).
- [ ] Integration tests with Postgres Testcontainers.

## Acceptance criteria
- Under concurrent attempts, only one purchase succeeds for a seat.
- Reservation TTL is enforced (expired reservations release seats).
