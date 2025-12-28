# Plan — waiting room with Redis Streams

## Goal
Implement a minimal Redis Streams waiting room with consumer groups and document pending-entry handling.

## Implemented design
- Stream: `waiting-room-joins`
- Consumer group: `granter` (created with `MKSTREAM`)
- Producer: HTTP join endpoint creates a `WAITING` session then `XADD`’s `{sessionId,eventId,userId}`
- Consumer: scheduled `XREADGROUP` poller activates sessions up to `waitingroom.capacity.max-active` and `XACK`s on successful activation

## Correctness notes
- Redis Streams consumer groups are at-least-once.
- Activation is idempotent.
- This minimal example does not implement `XCLAIM`/PEL recovery; it keeps the concept focused.

## Tests
- Unit tests: store validation and capacity
- Integration test: Redis Testcontainers + HTTP join → eventual `ACTIVE`
