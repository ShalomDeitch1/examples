# Plan — waiting room with RabbitMQ

## Goal
Implement a minimal waiting room with RabbitMQ and document consumer idempotency and capacity gating.

## Implemented design
- Exchange: `waiting-room` (direct)
- Queue: `waiting-room.joins`
- Message: plain `sessionId` (kept intentionally simple)
- Consumer: `@RabbitListener` pushes sessionIds into a backlog
- Granter: scheduled drain of backlog, activating sessions up to `waitingroom.capacity.max-active`

## Correctness notes
- RabbitMQ is at-least-once in practice (consumer restarts, redeliveries).
- Activation is idempotent, so duplicate deliveries don’t double-grant.

## Tests
- Unit tests: store validation and capacity
- Integration test: RabbitMQ Testcontainers + HTTP join → eventual `ACTIVE`
