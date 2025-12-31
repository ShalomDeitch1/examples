# Plan — waiting room (Token/Session bucket) with Redis Streams

## Goal
Implement a minimal **token/session** waiting room where users join, wait until they become `ACTIVE`, and then can proceed.

## Implemented design
- API creates a session record in Redis and enqueues the join into a Redis Stream.
- A scheduled grant loop periodically reads join records and activates sessions up to a fixed capacity.

### Redis keys
- Session hash: `waiting-room:session:{sessionId}`
  - `status` = `WAITING|ACTIVE|LEFT`
  - `eventId`, `userId`
- Active set: `waiting-room:active`
- Join stream: `waiting-room-joins`
- Cursor: `waiting-room:stream:last-id`

### Core correctness rules
- Never exceed `capacity` active sessions.
- Granting is idempotent (`markActive` can be called repeatedly).
- Leaving removes the session from the active set.

## Notes / intentionally omitted
- Session expiration / TTL cleanup.
- Redis Stream consumer group patterns (`XREADGROUP`, PEL recovery).

## Tests
- Integration test uses Testcontainers Redis and HTTP calls to verify:
  - up to `capacity` sessions become `ACTIVE`
  - after leaving, additional sessions can become `ACTIVE`
