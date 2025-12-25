# Waiting room with Redis Streams (consumer groups)

Redis Streams provides a low-moving-parts waiting room with consumer groups, which is a good fit for this repo’s “simple but real” philosophy.

## Tech choices
- Spring Boot 3.5.9 (Spring MVC), Java 21
- Redis Streams + consumer groups

## Core idea

- Stream: `waiting-room:{eventId}`
- Consumer group: `granter`
- Each join is an XADD entry containing `{sessionId, userId, createdAt}`.

## API sketch

- `POST /api/waiting-room/sessions` → `{sessionId}`
- `GET /api/waiting-room/sessions/{id}` → `{status}`

## Diagrams

```mermaid
flowchart LR
  U[User] --> TM[Ticketmaster API]
  TM --> R[(Redis Streams)]
  G[Granter consumer group] --> R
  G --> DB[(DB: sessions)]
```

```mermaid
sequenceDiagram
  participant U as User
  participant TM as Ticketmaster API
  participant R as Redis Streams
  participant G as Granter
  participant DB as DB

  U->>TM: POST /waiting-room/sessions
  TM->>DB: create WAITING
  TM->>R: XADD waiting-room:{eventId} sessionId
  TM-->>U: 202 {sessionId}

  G->>R: XREADGROUP (granter)
  G->>DB: set ACTIVE if capacity allows
  G->>R: XACK
```

## Trade-offs
- Pros: very simple to run locally (Redis already used in repo), consumer groups are “good enough”.
- Cons: replay/audit is weaker than Kafka; need to manage pending entries and consumer liveness.
