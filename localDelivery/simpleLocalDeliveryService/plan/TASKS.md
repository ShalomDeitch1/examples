# Tasks — simpleLocalDeliveryService

## Domain model
- Entities/tables:
  - `warehouses` (id, name, location)
  - `items` (id, name)
  - `inventory` (warehouse_id, item_id, available_qty, reserved_qty)
  - `customers` (id, name, location)
  - `orders` (id, customer_id, status, created_at)
  - `order_lines` (order_id, item_id, qty)

## Deliverability logic
- Implement a mock travel time function:
  - Compute distance (Haversine or simplified)
  - Convert to “minutes” using a constant speed
- An item is deliverable if at least one warehouse with stock is within the 1-hour threshold
- For simplicity, allow order fulfillment from multiple warehouses

## APIs
- `GET /items?customerId=...` or `GET /items?lat=..&lon=..`
  - returns only deliverable items
- `POST /orders`
  - request includes customer + items
  - reserves inventory immediately
  - creates order in `PENDING_PAYMENT`
- `POST /orders/{orderId}/confirm-payment`
  - mock payment confirmation
  - on success: move reserved -> consumed
  - on failure/timeout: release reservation

## Concurrency requirements
- Ensure no overselling under concurrent orders:
  - Use DB transactions + row locking on `inventory`
  - Or optimistic locking with retries

## Tests
- Unit tests for deliverability computations
- Concurrency tests:
  - multiple threads placing orders for the same SKU
  - assert inventory never goes negative
- Integration tests using Testcontainers Postgres

## Observability
- Add basic metrics/logging for:
  - cache-less list latency
  - order placement latency
