# Tasks — optimizedLocalDeliveryService

## Read path optimization (items)
- Implement 1km grid key
- Implement Redis cache:
  - key: `items:grid:{gridId}:v{version}`
  - TTL: 15 minutes
- Implement version key:
  - `items:grid:{gridId}:version`
- Cache rebuild logic on miss:
  - relevant warehouses
  - in-stock items
  - travel-time filter for 1-hour delivery window

## Warehouse relevance
- Implement one of:
  - PostGIS “warehouses within X km”
  - Redis GEO “within radius”
- Keep warehouse count small for demo, but code should be structured for scale

## Data sources
- Separate read vs write data access layers (optional):
  - reads -> replica datasource
  - writes -> primary datasource

## Write path correctness (orders)
- Reservation workflow:
  - transactionally reserve inventory in DB
  - create order in `PENDING_PAYMENT`
  - on payment success -> finalize
  - on payment failure/timeout -> release reservations
- Concurrency:
  - show lock strategy or optimistic concurrency with retry

## Cache invalidation/version bumps
- On inventory change:
  - bump relevant grid versions
- For the example, keep mapping from warehouse -> gridIds simple (precomputed set)

## Performance and correctness checks
- Load tests:
  - hit path for `GET /items`
  - concurrent `POST /orders`
- Add metrics/timers for latency goals
