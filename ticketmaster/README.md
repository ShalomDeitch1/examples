# Ticketmaster

This folder is a Ticketmaster-style system-design walkthrough, based on the HelloInterview breakdown:
https://www.hellointerview.com/learn/system-design/problem-breakdowns/ticketmaster

It is organized as **subprojects**. Each subproject is planning-only at this stage and contains:
- `README.md` (design + API sketch + diagrams + trade-offs)
- `plan.md` (a TODO list + acceptance criteria)

## Alignment with HelloInterview (high level)

What we aim to support in the “real implementation” design:
- Browse/view event details and seat availability (read-heavy, low latency).
- Purchase tickets without double-booking (consistency-first).
- Survive extreme spikes for high-demand events (waiting room + caching).

## Reuse existing examples (do not re-implement here)

These repo subprojects already teach the basics; Ticketmaster links to them instead of repeating them:

- Redis caching patterns and internal-cache discussion
  - `localDelivery/cachingWithRedisGeo/README.md` — Redis caching patterns and TTL thinking.
  - `localDelivery/postgresReadReplicas/README.md` — read scaling patterns that also apply to Ticketmaster browse.
  - `shortUrl/README.md` — internal vs external cache trade-offs.

- Postgres read replicas and cache versioning
  - `localDelivery/postgresReadReplicas/README.md` — replication trade-offs.
  - `localDelivery/cacheWithReadReplicas/README.md` — cache versioning to avoid stale replica reads.

- SQS/SNS style queueing pipeline (for comparison)
  - `dropbox/directS3/README.md` — SNS->SQS notification pipeline.
  - `dropbox/rollingChunks/README.md` — “durable DB feed + queue is only a hint” pattern.

## Subprojects

| Folder | What it covers | Key technologies |
|---|---|---|
| `tech-waiting-room/` | Waiting room + queueing trade-offs and design | SQS vs RabbitMQ vs Kafka vs Redis Streams |
| `tech-notifications/` | How users learn they’re out of the queue | Webhook, in-app notifications, SSE (Spring MVC) |
| `tech-locking-and-transactions/` | Prevent double booking + payment+ticket atomicity options | Postgres, DynamoDB, Redis locks, sagas, 2PC |
| `tech-caching-hotkeys/` | Read spikes, Redis pipelining, hot-key sharding + eviction | Redis, Caffeine |
| `simplest-implementation/` | “Works on my laptop” Ticketmaster | Spring MVC + Postgres |
| `real-implementation/` | “Real” design aligned to HelloInterview | Spring MVC + Postgres + Redis + Redis Streams + SSE |

## Suggested learning path

1) `tech-locking-and-transactions/` (consistency + correctness)
2) `tech-waiting-room/` (spike control)
3) `tech-notifications/` (client UX + correctness under retries)
4) `tech-caching-hotkeys/` (read scaling)
5) `simplest-implementation/` (baseline)
6) `real-implementation/` (put everything together)
