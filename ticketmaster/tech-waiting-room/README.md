# Waiting room (queueing) — overview

This subproject is an index of different queueing technologies that can implement a Ticketmaster **waiting room**.

The waiting room goal is to **meter access** to expensive/contended operations (seat selection + reservation) during spikes.

## Why waiting rooms exist

- Without metering, popular events create a thundering herd on the seat inventory rows and payment path.
- A waiting room converts “N concurrent seat-reserve attempts” into a **controlled concurrency** (e.g., only 200 active selectors).
- The core correctness rule is still enforced elsewhere: **never double-book a seat**.

## Options compared

This folder contains one subproject per option:
- `sqs/` — AWS SQS style queue (link to existing repo SQS examples in `dropbox/` from `ticketmaster/README.md`).
- `rabbitmq/` — classic AMQP broker.
- `kafka/` — log-based broker with consumer groups.
- `redis-streams/` — Redis Streams with consumer groups (chosen default for the “real implementation” in this repo).

## Trade-offs summary

| Option | Ordering | Delivery semantics | Consumer model | Replay | Ops complexity | Rough cost model | Typical fit |
|---|---:|---|---|---:|---:|---|---|
| SQS | best-effort (FIFO exists) | at-least-once | competing consumers | limited | low | pay per request (AWS-managed) | simple queueing, AWS-native |
| RabbitMQ | strong per-queue | at-least-once | queues/exchanges | limited | medium | run broker (infra cost) | work queues, routing |
| Kafka | per-partition | at-least-once (exactly-once possible) | consumer groups | yes | medium-high | run cluster (infra + ops) | streams, replay, auditing |
| Redis Streams | per-stream | at-least-once | consumer groups | limited | low-medium | piggyback on Redis | simple streams, low parts |

Notes:
- “Rough cost model” is intentionally non-numeric: exact dollars depend on traffic, retention, and ops.

## API sketch (shared)

All implementations can share the same external API shape:
- `POST /api/waiting-room/sessions` → join queue, returns `waitingRoomSessionId`
- `GET /api/waiting-room/sessions/{id}` → status (`WAITING` / `ACTIVE` / `EXPIRED`)
- `POST /api/waiting-room/sessions/{id}:heartbeat` → keep session alive

## Links

- SQS implementation plan: `sqs/README.md`
- RabbitMQ implementation plan: `rabbitmq/README.md`
- Kafka implementation plan: `kafka/README.md`
- Redis Streams implementation plan (default): `redis-streams/README.md`
