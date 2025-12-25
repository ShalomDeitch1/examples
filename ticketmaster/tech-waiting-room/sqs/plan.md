# Plan — waiting room with SQS

## Goal
Implement a minimal waiting room using SQS-style queueing and document correctness + failure modes.

## TODO
- [ ] Define `WaitingRoomSession` table schema and statuses.
- [ ] Add controller endpoints to join and poll session status.
- [ ] Add SQS producer to enqueue join events.
- [ ] Add a worker/consumer that grants permits up to `maxActiveSelectors`.
- [ ] Add idempotency to join (e.g., `Idempotency-Key` header).
- [ ] Add integration test using LocalStack.

## Acceptance criteria
- Joining creates a WAITING session and enqueues a message.
- Worker grants ACTIVE status respecting max active concurrency.
- Polling returns deterministic status transitions.
