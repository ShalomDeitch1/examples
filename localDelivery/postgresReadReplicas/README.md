# postgresReadReplicas

This subproject demonstrates **PostgreSQL streaming replication** to create **read replicas** for read-heavy operations.

Goal:
- Route read-heavy endpoints (like listing items) to replicas
- Keep write endpoints (placing orders) on the primary for correctness

## Architecture

```mermaid
graph TB
  Client[Client] --> LB[Load Balancer]
  LB --> ReadAPI["Read API (list items)"]
  LB --> WriteAPI["Write API (place order)"]

  WriteAPI --> Primary[(Postgres Primary)]
  Primary -->|Streaming replication| Replica1[(Read Replica 1)]
  Primary -->|Streaming replication| Replica2[(Read Replica 2)]
  ReadAPI --> Replica1
  ReadAPI --> Replica2
```

## How to Run

Prereqs:
- Docker
- Java 21+
- Maven

Start primary + replicas:

```bash
docker compose up -d
```

Run the sample app:

```bash
mvn spring-boot:run
```

Quick smoke tests (after the app is running)

- List deliverable items for a location (lat/lon):

```bash
curl -sS "http://localhost:8080/items?lat=40.7128&lon=-74.0060" | jq .
```

- Place an order (example):

```bash
curl -sS -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{ "customerId":"<customer-uuid>", "lines":[{ "itemId":"<item-uuid>", "quantity":1 }] }' | jq .
```

- Confirm payment:

```bash
curl -sS -X POST http://localhost:8080/orders/<order-uuid>/confirm-payment \
  -H 'Content-Type: application/json' \
  -d '{ "success": true }' | jq .
```

Note: replica reads are used for `GET /items` by default; expect possible eventual consistency after writes.

## Trade-offs / Notes

- Replication is eventually consistent (replica lag).
- Read-your-writes is not guaranteed when reading from replicas immediately after a write.

## Task list

See [plan/TASKS.md](./plan/TASKS.md).
