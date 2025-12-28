# Waiting room with RabbitMQ

RabbitMQ implements a waiting room as a classic work-queue: join requests are messages; consumers grant permits.

## Tech choices
- Spring Boot 3.5.9 (Spring MVC), Java 21
- RabbitMQ (Testcontainers for local testing)

## API sketch

- `POST /api/waiting-room/sessions` → `{waitingRoomSessionId}`
- `GET /api/waiting-room/sessions/{id}` → `{status}`

## Diagrams

```mermaid
flowchart LR
  U[User] --> TM[Ticketmaster API]
  TM --> X[RabbitMQ exchange]
  X --> Q[Queue: waiting-room]
  C[Consumer] --> Q
  C --> DB[(DB: sessions)]
  U --> TM
  TM --> DB
```

```mermaid
sequenceDiagram
  participant U as User
  participant TM as Ticketmaster API
  participant MQ as RabbitMQ
  participant C as Consumer
  participant DB as DB

  U->>TM: POST /waiting-room/sessions
  TM->>DB: create session WAITING
  TM->>MQ: publish(join, sessionId)
  TM-->>U: 202 {sessionId}

  C->>MQ: consume(join)
  C->>DB: set ACTIVE if capacity allows
  C-->>MQ: ack
```

## Trade-offs
- Pros: strong work-queue semantics; flexible routing.
- Cons: operating a broker; limited replay compared to Kafka; at-least-once delivery → need idempotent consumers.

## Run tests

```bash
./test.sh
```

## Run locally

For a manual run you need a reachable RabbitMQ broker (the tests use Testcontainers).

```bash
./run.sh
```

## Try it (curl)

Join and capture `sessionId`:

```bash
curl -s -XPOST localhost:8080/api/waiting-room/sessions \
  -H 'content-type: application/json' \
  -d '{"eventId":"E1","userId":"U1"}'
```

Poll for status (replace `<sessionId>`):

```bash
curl -s localhost:8080/api/waiting-room/sessions/<sessionId>
```
