# simpleLocalDeliveryService

This subproject is a **simple, correct** local delivery service.

Scope:
- Warehouses with inventory
- Customers can place orders
- Orders reserve inventory while payment is pending (mock payment)
- Deliverability is computed with a **mock travel time** service based on distance (no caching yet)

Non-functional goals (in this simplified version):
- Keep the code clean and testable
- Demonstrate the baseline behavior before applying caching/geo optimizations

## Architecture

```mermaid
graph TB
  Client[Client] --> ItemsAPI[Items API]
  Client --> OrdersAPI[Orders API]

  ItemsAPI --> Service[Delivery Eligibility Service]
  Service --> Travel[Mock Travel Time Service]
  Service --> DB[(Relational DB)]

  OrdersAPI --> Payment[Mock Payment Service]
  OrdersAPI --> DB
```

## How to Run

Prereqs:
- Docker
- Java 21+
- Maven

Start Postgres:

```bash
docker compose up -d
```

Run the app:

```bash
mvn spring-boot:run
```

Quick smoke tests (after the app is running)

- List deliverable items for a location (lat/lon):

```bash
curl -sS "http://localhost:8096/items?lat=40.7128&lon=-74.0060" | jq .
```

- List deliverable items for a customer (customerId):

```bash
curl -sS "http://localhost:8096/items?customerId=20000000-0000-0000-0000-000000000001" | jq .
```

- Place a simple order (replace UUIDs):

```bash
curl -sS -X POST http://localhost:8096/orders \
  -H 'Content-Type: application/json' \
  -d '{ "customerId":"20000000-0000-0000-0000-000000000001", "lines":[{ "itemId":"10000000-0000-0000-0000-000000000001", "qty":1 }] }' \
  -o /dev/stdout | jq .
```
Note: to capture the created order's `orderId` and reuse it in the confirm-payment request, run:

```bash
# Save the POST response and show it
resp=$(curl -sS -X POST http://localhost:8096/orders \
  -H 'Content-Type: application/json' \
  -d '{ "customerId":"20000000-0000-0000-0000-000000000001", "lines":[{ "itemId":"10000000-0000-0000-0000-000000000001", "qty":1 }] }')
echo "$resp" | jq .

# Extract orderId into an env var (bash/sh)
export ORDER_ID=$(echo "$resp" | jq -r '.orderId')
echo "ORDER_ID=$ORDER_ID"

# Confirm payment using the extracted ORDER_ID
curl -sS -X POST "http://localhost:8096/orders/${ORDER_ID}/confirm-payment" \
  -H 'Content-Type: application/json' \
  -d '{ "success": true }' | jq .
```


## Trade-offs / Notes

- No caching yet: item listing might not meet 100ms in worst cases.
- Prioritizes correctness for orders: use transactions/locking to avoid overselling.

## Task list

See [plan/TASKS.md](./plan/TASKS.md).
