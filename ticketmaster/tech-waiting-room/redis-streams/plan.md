# Plan — waiting room with Redis Streams

## Goal
Implement a minimal Redis Streams waiting room with consumer groups and document pending-entry handling.

## TODO
- [ ] Define stream naming and retention strategy.
- [ ] Implement join producer (XADD).
- [ ] Implement granter consumer group (XREADGROUP + XACK).
- [ ] Handle stuck pending entries (XCLAIM) and consumer crash recovery.
- [ ] Integration tests using Redis Testcontainers.

## Acceptance criteria
- Joins are processed at-least-once and grant logic is idempotent.
- Pending entries are reclaimed and processed.
