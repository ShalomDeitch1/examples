# Plan — waiting room with SQS

## Goal
Implement a minimal waiting room using SQS-style queueing and document correctness + failure modes.

## Implemented
- Spring Boot app exposing a small HTTP API for joining and polling.
- In-memory `WaitingRoomStore` tracking sessions and status transitions.
- `WaitingRoomJoinPublisher` that enqueues join requests into SQS (message body = `sessionId`).
- `SqsGrantPoller` scheduled poller that:
	- receives messages
	- activates sessions when capacity allows
	- deletes consumed messages
- Queue auto-creation for local/dev via `QueueUrlProvider`.

## Acceptance criteria
- Joining creates a WAITING session and enqueues a message.
- Worker grants ACTIVE status respecting max active concurrency.
- Polling returns deterministic status transitions.

## Correctness notes
- Capacity gating is enforced at grant time using `WaitingRoomStore.tryActivateIfCapacityAllows(...)`.
- Activation is idempotent (re-activating an already ACTIVE session is a no-op).
- On capacity full, the poller leaves the message in the queue (no delete), so it can be retried.

## Tests
- Unit: store validation and capacity gating.
- Integration: LocalStack (Testcontainers) validates HTTP join -> SQS send -> poller activation -> ACTIVE.
