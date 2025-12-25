# Postgres locking — plan (minimal)

Goal: runnable, testable examples of **pessimistic vs optimistic** locking.

## TODO

- [ ] Create Maven module (Java 21 + Spring Boot 3.5.9)
- [ ] No web server: keep it library-style + tests
- [ ] One table: `seat_inventory(event_id, seat_id, status, version)`
- [ ] Implement pessimistic reserve: `SELECT ... FOR UPDATE` then `UPDATE`
- [ ] Implement optimistic reserve: `UPDATE ... WHERE version = ?`
- [ ] Add 2 concurrency tests proving exactly one winner

## Acceptance criteria

- `mvn test` passes (with Docker running)
- Both approaches prevent double-reservation
