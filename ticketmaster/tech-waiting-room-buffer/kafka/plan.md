# Plan — waiting room with Kafka

## Goal
Provide a minimal but complete waiting-room implementation using Kafka, focusing on consumer groups, at-least-once delivery, and idempotent “grant permit” handling.

## Implemented design
- Topic: `waiting-room-joins`
- Producer: HTTP join endpoint creates a `WAITING` session then produces `sessionId` keyed by `eventId`
- Consumer: `@KafkaListener` appends received `sessionId` into an in-memory backlog
- Granter: a scheduled task drains the backlog and activates sessions up to `waitingroom.capacity.max-active`

## Correctness notes
- At-least-once delivery means duplicates are possible.
- Activation is idempotent: activating an already `ACTIVE` session is a no-op.

## Tests
- Unit tests: in-memory store validation and capacity behavior
- Integration test: Kafka Testcontainers + HTTP join → eventual `ACTIVE`
