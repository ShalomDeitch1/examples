---
description: Prompt to plan Ticketmaster folder (tech explainers + simplest implementation + real implementation)
agent: agent
---

You are working in a repository organized into subprojects, each demonstrating a specific system-design concept.

# Goal
Inside the folder `ticketmaster/` create:
- a top-level `ticketmaster/README.md` that is an index/overview (no deep dives, no Mermaid diagrams)
- several Ticketmaster **subprojects**, each with exactly **two files**:
  - `README.md`
  - `plan.md` (a TODO list)

This stage is **planning only**: do **not** implement code yet.

# Global constraints
- Language/framework: **Java + Spring Boot (Spring MVC only)**.
  - No WebFlux.
  - The “user app” examples for notifications etc. must be Spring MVC sprigboot 3.5.9, java 21.
- Keep implementations **as simple as possible**. Examples of technologies can explain how to scale. The "real implementation" should be scalable, low latency, and robust, but keep the number of moving parts as low as possible to achieve that.
- Avoid duplicating technologies already demonstrated elsewhere in this repo.
  - If a technology is already covered, reference it from `ticketmaster/README.md` with a short note explaining why it matters for Ticketmaster, but **do not** create a Ticketmaster subproject that re-teaches it.
- **Do not refer to** `external-cache/` anywhere.
- Use clear, readable Mermaid diagrams (flowchart / sequence) and label them well.
- Each subproject’s `plan.md` must contain:
  - a short goal statement
  - a checklist-style TODO list
  - acceptance criteria (what “done” means)

# Real-implementation requirements source
The Ticketmaster “real implementation” requirements MUST explicitly reflect (and link to):
- https://www.hellointerview.com/learn/system-design/problem-breakdowns/ticketmaster

In `ticketmaster/real-implementation/README.md`, include a short section titled **“Alignment with HelloInterview”** summarizing the functional and non-functional requirements you are implementing (high-level; do not quote large text).

Also in that README, include **“Why these choices”**: explain why the selected technologies were selected. The reason can be they show a good implementation but if needed they can be the  *easiest to implement and test in this repo context* to keep things relatively clear.

# Technologies that already exist in this repo (link, don’t re-teach)
In `ticketmaster/README.md`, include a section **“Reuse existing examples (do not re-implement here)”** with links and 1–2 sentence notes explainiing why they matter for Ticketmaster. Examples to link to:
- Redis caching patterns and internal-cache discussion: link to
  - `localDelivery/cachingWithRedisGeo/README.md`
  - `localDelivery/postgresReadReplicas/README.md`
  - `shortUrl/README.md`
- Postgres read replicas and cache versioning: link to
  - `localDelivery/postgresReadReplicas/README.md`
  - `localDelivery/cacheWithReadReplicas/README.md`
- SQS/SNS style queueing pipeline (for comparison): link to
  - `dropbox/directS3/README.md`
  - `dropbox/rollingChunks/README.md`

Important: You will still use Redis/Postgres/SQS concepts where needed in Ticketmaster docs, but you won’t create subprojects that are just “Redis basics” or “SQS basics”.

# Ticketmaster domain (for docs)
We are building a simplified Ticketmaster-like system:
- Events, venues, and seats
- Users can browse/search, view event details and seat availability, and purchase seats
- High-demand events need a **waiting room** to smooth bursts
- Prevent double-booking at all costs

Suggested simplified data model terms (use in docs):
- `Event(eventId, venueId, startsAt, ...)`
- `Seat(seatId, venueId, section, row, number)`
- `SeatInventory(eventId, seatId, status, version, reservedUntil, orderId)`
- `Order(orderId, userId, eventId, status, expiresAt, total, ...)`

# Features that MUST be covered (with examples + diagrams + exampple sub-projects, each with README.md + plan.md)

## 1) Waiting Room (high demand queue)
Requirement:
- When demand spikes, requests to book tickets must be queued.
- Users must be told when they are **out of the waiting room** and allowed to select seats.

Docs must include:
- Compare and contrast **SQS vs RabbitMQ vs Kafka vs Redis Streams with consumer groups** for the waiting room.
  - Provide a small table of trade-offs: ordering guarantees, consumer groups, replays, operational complexity, delivery semantics, scaling, price in $.
  - Link to existing SQS examples (above) for SQS background.
- Choose **Redis Streams with consumer groups** as the primary implementation for the “real implementation”.

Mermaid required:
- Flow diagram: user requests -> waiting room -> ticket selection capacity gate
- Sequence diagram: enqueue, poll/notify, proceed to selection

## 2) Notifications when user leaves the waiting room
Requirement:
When a user is placed in a waiting room, the user needs to know when they are out.
Show multiple approaches:
- Webhook callback with a simplistic **Spring MVC user app**
- Notification to the user via a simplistic **Spring MVC user app**
- Server-Sent Events (SSE) with a simplistic **Spring MVC user app**

Docs must include:
- For each approach: how the server tracks correlation (userId / sessionId), retries, idempotency, security (HMAC signature for webhook), and failure modes.
- For SSE: how to scale fanout conceptually (but keep implementation simple), timeouts, reconnect behavior, and event IDs.

Mermaid required:
- Sequence diagrams for each of webhook, app-notification, SSE

## 3) Payment + ticket issuance in a transaction (locking examples)
Requirement:
Payments and getting a ticket should be in a transaction.
Provide examples of different ways to lock:
1) For systems small enough — in the database:
   - Postgres example
   - DynamoDB example (must be explained here since we are not referencing external-cache)
2) Show optimistic and pessimistic locking
3) When data is spread between more than one database, show each of:
   - external lock (e.g. Redis-based distributed lock)
   - sagas
   - two-phase commit (2PC)

Docs must include:
- Postgres:
  - Pessimistic: `SELECT ... FOR UPDATE` seat row(s) and order row
  - Optimistic: version column (`version`) and `UPDATE ... WHERE version = ?`
  - Mention transaction isolation choices (keep it practical; default to READ COMMITTED unless you justify otherwise)
- DynamoDB:
  - Conditional updates (`ConditionExpression`) and transactional writes (`TransactWriteItems`)
  - Idempotency keys
- Multi-DB:
  - Redis distributed lock example (with lease time, renewal, release)
  - A saga example (orchestration) with compensations (release reservation, refund/void authorization)
  - 2PC example (educational) and why it’s usually avoided (operational fragility)

Mermaid required:
- Sequence diagram: Redis distributed lock flow
- Sequence diagram: reserve seat -> authorize payment -> capture -> ticket issued
- Sequence diagram: saga path with compensations
- Small diagram: 2PC coordinator overview (educational)

## 4) Spikes: caching + pipelining to reduce traffic (Redis)
Requirement:
Sometimes there are spikes with lots of calls.
Give example of caching + **pipelining** calls to reduce traffic (use Redis as the example).

Docs must include:
- What is cached (event details, seat map summary, availability summary)
- Cache key strategy and TTL
- Example of Redis pipelining (conceptual + pseudo-code; no code implementation yet)

## 5) Two-tier caching: internal short TTL + external longer TTL
Requirement:
Since there can be very high spikes, show:
- internal caches with a short TTL that go to an external cache with a longer TTL
- an example of finding high demand items and adding a suffix to the key so access is spread over different servers in a cluster
- how data would be evicted if there was a change to the value of such a hot-key

Docs must include:
- Internal cache example: Caffeine cache (short TTL)
- External cache example: Redis (longer TTL)
- Hot key sharding pattern:
  - Explain a scheme like `availability:{eventId}:{version}:{shard}`
  - Define how `shard` is chosen (e.g., hash(userId) % N, consistent hashing)
  - Define how to invalidate on change: bump `version` and/or publish invalidation event; explain how old keys age out

Mermaid required:
- Flow diagram: request -> internal cache -> external cache -> DB
- Flow diagram: invalidation on seat purchase

## 6) Two-phase payments (realistic)
Requirement:
Show two-phase payments:
- make order
- confirm order and authorize payment
- capture payment when issuing ticket

Docs must include:
- A production-style client interaction:
  - client creates order intent
  - client confirms order (provides payment method token)
  - server authorizes payment
  - server captures payment only once seats are finalized
  - client gets final status via SSE/notification/polling
- Idempotency keys for each step
- How to handle timeouts and retries without double-charging

Mermaid required:
- Sequence diagram: client ↔ ticketmaster ↔ payment provider

# Output structure to generate
Create the following files under `ticketmaster/`.

## Top-level index
1) `ticketmaster/README.md`
- Purpose of this folder (1–2 paragraphs)
- “Alignment with HelloInterview” (brief overview)
- “Reuse existing examples (do not re-implement here)” section with links listed above
- Table listing all Ticketmaster subprojects: folder → what it teaches → key technologies → what’s the simplest runnable target later
- Suggested learning path ordering
- README.md should refer to each subproject in an order that helps the reader understand the technologies and how they fit together and can be used in the "real implementation".

## Subprojects (each must contain only README.md + plan.md)

### A) `ticketmaster/tech-waiting-room/`
- a subproject for each different technology explained above (SQS, RabbitMQ, Kafka, Redis Streams with consumer groups)
- the README.md should link to each implementation and explain pros and cons for each technology
- each subproject should have its own README.md + plan.md
- each subproject README.md must include Mermaid diagrams as required above
- tech-waiting-room's README.md: waiting room design + Redis Streams with consumer groups default; compare SQS/RabbitMQ/Kafka/Redis Streams with consumer groups fairly


### B) `ticketmaster/tech-notifications/`
- should contain a sub-project for each notification approach explained above
- each subproject should have its own README.md + plan.md
- each subproject README.md must include Mermaid diagrams as required above
- tech-notifications's README.md should link to each implementation and explain trade-offs


### C) `ticketmaster/tech-locking-and-transactions/`
- should contain a sub-project for each locking/transaction approach explained above:
  - Postgres locking (optimistic + pessimistic)
  - DynamoDB locking (conditional updates + transactions)
  - Multi-DB locking/transactions (Redis distributed lock, saga, 2PC)
  - each subproject should have its own README.md + plan.md
  - each subproject README.md must include Mermaid diagrams as required above
  - tech-locking-and-transactions's README.md must link to each implementation and explain trade-offs

### D) `ticketmaster/tech-caching-hotkeys/`
- README.md: Redis pipelining; internal+external caches; hot-key sharding + eviction
- plan.md: TODO list for implementing caching layers later

### E) `ticketmaster/simplest-implementation/`
Goal: the most simple Ticketmaster that works, even if it cannot scale.
- README.md must specify:
  - Single Spring MVC service
  - Single Postgres database
  - Very simple seat reservation + purchase path (no Kafka)
  - No waiting room (or an in-memory queue)
  - Basic locking strategy (choose one) and why
  - Minimal endpoints
  - What it cannot handle (explicitly)
- plan.md must be a TODO list to implement it later with unit tests

### F) `ticketmaster/real-implementation/`
Goal: a “real” implementation aligned with the HelloInterview Ticketmaster breakdown.
- README.md must include:
  - “Alignment with HelloInterview” section (functional + non-functional requirements)
  - A clear architecture choice that is easiest to implement and test here:
    - Spring MVC services (can be modular but keep count small)
    - Redis Streams with consumer groups waiting room
    - Postgres as source of truth for seat inventory
    - Redis for caching + reservation TTL 
    - SSE for client updates (Spring MVC)
  - “Why these choices” section (ease-of-implementation + ease-of-testing)
  - End-to-end API flow: browse -> waiting room -> seat selection -> reserve -> authorize -> capture -> ticket issued
  - Clear failure-mode handling: retries, idempotency, duplicate messages
- plan.md must be a TODO list broken down by component (API, DB schema, Kafka, caching, SSE, payments) with acceptance criteria

# Style requirements for all READMEs
- Keep prose concise and practical.
- Every README must have:
  - **Tech choices** (bullet list)
  - **API sketch** (endpoints + request/response summary)
  - **Mermaid diagrams** as required above
  - **Trade-offs** and why something was chosen

# Testing
 - all logic should have unit tests
 - integration tests should show that all works together
 - there should be at least one test for each subproject with code
 - there should be a way to run each server, if needed a docker compose file and a run.sh and test.sh file to run and test the code in each subproject as needed
 - each README.md file, ensure that there are instructions of how to test the system using curl commands
 - if there is a need to pass some value from a previous step, ensure that the user can do copy & paste to get the result they need

# Final check
Before finishing, verify:
- You did not reference `external-cache/` anywhere.
- Every subproject has exactly `README.md` + `plan.md`.
- `ticketmaster/real-implementation/README.md` clearly ties to HelloInterview and explains choices.
- the README.md and plan.md files are explicit enough to implement later.
- use the skill files `design` and `implementation` to guide your writing style and structure.

