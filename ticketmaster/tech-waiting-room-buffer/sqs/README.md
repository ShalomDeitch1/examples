# Waiting room with SQS

SQS can implement a waiting room by buffering “join requests” and letting a scheduled poller process them in small batches.

## Tech choices
- Spring Boot 3.5.9 (Spring MVC), Java 21
- AWS SQS via AWS SDK v2 (or LocalStack for local testing)

## Core idea

1) User enqueues a request → server creates a `WAITING` request and sends a message with body = `requestId`.
2) A scheduled poller receives up to `N` messages per tick (max 10 for SQS ReceiveMessage) and marks them processed.
3) Client polls observability to see counts and batch progress.

## API (shared)

This module uses the shared 2-endpoint API:
- `POST /api/waiting-room/requests` → `{ requestId }`
- `GET /api/waiting-room/observability` → counts + processing batches

## Diagrams

```mermaid
flowchart LR
  U[User] -->|POST /requests| TM[Waiting Room API]
  TM -->|SendMessage| Q[SQS queue]
  W[Poller] -->|ReceiveMessage| Q
  W -->|mark processed| S[(Request Store)]
  W -->|record batch| H[(Processing History)]
  U -->|GET /observability| TM
  TM --> S
  TM --> H
```

```mermaid
sequenceDiagram
  participant U as User
  participant TM as Waiting Room API
  participant Q as SQS
  participant W as Poller
  participant S as Store

  U->>TM: POST /api/waiting-room/requests
  TM->>S: create WAITING request
  TM->>Q: SendMessage(body=requestId)
  TM-->>U: 202 {requestId}

  W->>Q: ReceiveMessage
  W->>S: markProcessed(requestId)
  W->>Q: DeleteMessage

  U->>TM: GET /api/waiting-room/observability
  TM->>S: read counts
  TM-->>U: {counts, batches}
```

## Trade-offs
- Pros: easy mental model, managed in AWS.
- Cons: limited replay semantics; ordering depends on using FIFO queues; extra moving parts (worker + persistence).

## Running

Local SQS is easiest via LocalStack.

1) Start LocalStack SQS (Docker):

```bash
docker run --rm -p 4566:4566 localstack/localstack:latest
```

2) Run the app (config in `src/main/resources/application.yml`):

```bash
./run.sh
```

## Testing

Integration test uses Testcontainers + LocalStack.

```bash
./test.sh
```

## Curl

```bash
curl -s -X POST "http://localhost:8080/api/waiting-room/requests" \
  -H "content-type: application/json" \
  -d '{"eventId":"E1","userId":"U1"}'
```

Then observe:

```bash
curl -s "http://localhost:8080/api/waiting-room/observability"
```
