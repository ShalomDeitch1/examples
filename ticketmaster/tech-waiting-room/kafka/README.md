# Waiting room with Kafka

Kafka supports a waiting room as a stream where consumers grant permits. Kafka shines when you want **replay**, auditability, and multiple independent consumers.

## Tech choices
- Spring Boot 3.5.9 (Spring MVC), Java 21
- Kafka (Testcontainers)

## Core idea

- Topic: `waiting-room-joins`
- Key: `eventId` (keeps an event’s joins ordered per partition)
- Consumer group: `waiting-room-granter`

## API sketch

- `POST /api/waiting-room/sessions` → `{sessionId}`
- `GET /api/waiting-room/sessions/{id}` → `{status}`

## Diagrams

```mermaid
flowchart LR
  U[User] --> TM[Ticketmaster API]
  TM --> K[(Kafka topic: waiting-room-joins)]
  G[Grant consumer group] --> K
  G --> DB[(DB: sessions)]
```

```mermaid
sequenceDiagram
  participant U as User
  participant TM as Ticketmaster API
  participant K as Kafka
  participant G as Granter
  participant DB as DB

  U->>TM: POST /waiting-room/sessions
  TM->>DB: create WAITING
  TM->>K: produce(eventId key, sessionId)
  TM-->>U: 202 {sessionId}

  G->>K: poll
  G->>DB: set ACTIVE if capacity allows
  G->>K: commit offset
```

## Trade-offs
- Pros: replay, multiple consumers, strong per-partition ordering.
- Cons: heavier ops than Redis Streams; more concepts (partitions, offsets) for a simple waiting room.
