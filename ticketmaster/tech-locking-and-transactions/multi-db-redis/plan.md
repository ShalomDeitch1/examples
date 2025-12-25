```markdown
# Plan — multi-db-redis (minimal)

Goal: runnable, testable example of a **safe Redis lock** (token + Lua release).

## TODO
- [ ] `tryAcquire` using `SET NX PX`
- [ ] `release` using Lua token-check
- [ ] Integration tests with Redis Testcontainers

## Acceptance criteria
- `mvn test` passes (with Docker running)
- Only one contender can acquire a lock
- Non-owner cannot release
```
