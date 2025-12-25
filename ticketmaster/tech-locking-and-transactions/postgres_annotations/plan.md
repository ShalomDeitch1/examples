# Postgres locking (annotations) — plan (minimal)

Goal: same concurrency behavior as `../postgres/`, but expressed using Spring annotations + JPA.

## TODO

- [ ] Create Maven module (Java 21 + Spring Boot 3.5.9)
- [ ] Map `seat_inventory` using JPA annotations
- [ ] Implement pessimistic reserve via `@Transactional` + `@Lock(PESSIMISTIC_WRITE)`
- [ ] Implement optimistic reserve via version-checked update
- [ ] Add 2 concurrency tests proving exactly one winner

## Acceptance criteria

- `mvn test` passes (with Docker running)
- Module is self-contained (no dependency on `../postgres/`)
