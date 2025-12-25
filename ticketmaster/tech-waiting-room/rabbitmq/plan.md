# Plan — waiting room with RabbitMQ

## Goal
Implement a minimal waiting room with RabbitMQ and document consumer idempotency and capacity gating.

## TODO
- [ ] Define join message format and exchange/queue names.
- [ ] Implement join endpoint + persistence.
- [ ] Implement consumer that grants ACTIVE up to a limit.
- [ ] Implement retry handling (dead-letter / requeue rules).
- [ ] Integration tests using RabbitMQ Testcontainers.

## Acceptance criteria
- Consumer is idempotent (replay of same join message doesn’t grant multiple permits).
- Capacity limit is enforced.
