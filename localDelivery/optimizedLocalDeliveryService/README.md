# optimizedLocalDeliveryService

This subproject is an optimized version of the local delivery service.

Target behaviors:
- `GET /items` aims for ~100ms by using:
  - 1km x 1km **grid clustering** for location-based keys
  - Redis caching for “deliverable items per grid” (TTL 15 minutes)
  - Redis GEO / PostGIS to find relevant warehouses quickly
  - Postgres read replicas for scaling reads

- `POST /orders` aims for ~2s and stays consistent by using primary DB transactions for inventory reservations.

## Architecture

```mermaid
graph TB
  Client[Client] --> ItemsAPI[Items API]
  Client --> OrdersAPI[Orders API]

  ItemsAPI --> Grid[Grid mapper 1km x 1km]
  ItemsAPI --> Cache[("Redis cache\nitems:grid:{id}\nTTL 15m")]
  Cache -- hit --> ItemsAPI
  Cache -- miss --> Warehouses[Relevant warehouses query]
  Warehouses --> Geo[(PostGIS or Redis GEO)]
  ItemsAPI --> Travel[Mock travel time]
  ItemsAPI --> ReadDB[("Read replica(s)")]
  ItemsAPI --> Cache

  OrdersAPI --> Primary[(Postgres primary)]
  OrdersAPI --> Payment[Mock payment]
  OrdersAPI --> CacheVersion[(Redis version keys)]
```

## How to Run

Prereqs:
- Docker
- Java 21+
- Maven

Start infra:

```bash
docker compose up -d
```

Run the app:

```bash
mvn spring-boot:run
```

Quick smoke tests (after the app is running)

- Get deliverable items for a grid/location (cache + geo):

```bash
curl -sS "http://localhost:8080/items?lat=40.7128&lon=-74.0060" | jq .
```

- Place an order (writes to primary, bumps version):

```bash
curl -sS -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{ "customerId":"<customer-uuid>", "lines":[{ "itemId":"<item-uuid>", "quantity":1 }] }' | jq .
```

- Confirm payment for an order:

```bash
curl -sS -X POST http://localhost:8080/orders/<order-uuid>/confirm-payment \
  -H 'Content-Type: application/json' \
  -d '{ "success": true }' | jq .
```

Notes:
- Cache TTL: 15 minutes for `items:grid:{id}:v{ver}` keys.
- Warehouses are indexed into Redis GEO at startup by `WarehouseGeoIndexer`.

## Trade-offs / Notes

- Optimizes for the read SLA using cache + precomputation.
- Writes remain consistent but require careful locking / reservation patterns.
- Versioned caching reduces stale reads but adds complexity.

## Task list

See [plan/TASKS.md](./plan/TASKS.md).
