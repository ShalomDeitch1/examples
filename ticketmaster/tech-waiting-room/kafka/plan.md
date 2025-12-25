# Plan — waiting room with Kafka

## Goal
Implement a minimal Kafka-based waiting room and document ordering + replay + idempotency.

## TODO
- [ ] Define topic, partitioning strategy, and message schema.
- [ ] Implement producer (join) and consumer (grant permits).
- [ ] Implement idempotent grant logic.
- [ ] Integration tests using Kafka Testcontainers.

## Acceptance criteria
- Consumer group enforces a stable grant order per event partition.
- Replays do not create duplicate grants.
