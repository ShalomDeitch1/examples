# Ticketmaster

This folder is a Ticketmaster-style system-design walkthrough, based on the HelloInterview breakdown:
https://www.hellointerview.com/learn/system-design/problem-breakdowns/ticketmaster

It is organized as **subprojects**. Each subproject presents either a concept useful for implementation, or an implementation

## Alignment with HelloInterview (high level)

What we aim to support in the “real implementation” design:
- Browse/view event details and seat availability (read-heavy, low latency).
- Purchase tickets without double-booking (consistency-first).
- Survive extreme spikes for high-demand events (waiting room + caching).

## useful links from other projects

- Redis caching patterns and internal-cache discussion
  - [localDelivery/cachingWithRedisGeo/](../localDelivery/cachingWithRedisGeo) — Redis caching patterns and TTL thinking.
  - [localDelivery/postgresReadReplicas](../localDelivery/postgresReadReplicas) — read scaling patterns that also apply to Ticketmaster browse.
  - [shortUrl](../shortUrl) — internal vs external cache trade-offs.

- Postgres read replicas and cache versioning
  - [localDelivery/postgresReadReplicas](../localDelivery/postgresReadReplicas) — replication trade-offs.
  - [localDelivery/cacheWithReadReplicas](../localDelivery/cacheWithReadReplicas) — cache versioning to avoid stale replica reads.


## Subprojects

| Folder | What it covers | Key technologies |
|---|---|---|
| [tech-locking-and-transactions/](tech-locking-and-transactions/) | Prevent double-booking; demonstrates sagas and multi-DB transaction patterns | Postgres, DynamoDB, Redis locks, sagas, 2PC |
| `tech-waiting-room/` | Waiting room + queueing trade-offs and design | SQS vs RabbitMQ vs Kafka vs Redis Streams |
| `tech-notifications/` | How users learn they’re out of the queue | Webhook, in-app notifications, SSE (Spring MVC) |
| `tech-caching-hotkeys/` | Read spikes, Redis pipelining, hot-key sharding + eviction | Redis, Caffeine |
| `simplest-implementation/` | “Works on my laptop” Ticketmaster | Spring MVC + Postgres |
| `real-implementation/` | “Real” design aligned to HelloInterview | Spring MVC + Postgres + Redis + Redis Streams + SSE |

