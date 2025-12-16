# Tasks — cachingWithRedisGeo

## Core concepts to implement
- 1km x 1km grid key derivation from (lat, lon)
- Cache key structure:
  - `deliverable-items:grid:{gridId}`
  - Optional: include a version segment `v{n}`
- Cache TTL: 15 minutes

## Data sources
- Warehouse locations (PostGIS or Redis GEO)
- Inventory per warehouse
- Mock travel time service:
  - input: origin (warehouse) + destination (grid center)
  - output: travel time (seconds)

## Read path (list items)
- Given a customer lat/lon:
  1) compute gridId
  2) read from Redis
  3) on miss, compute deliverable items, store in Redis with TTL
- Ensure the cached list contains only items deliverable within 1 hour

## API endpoints
- `GET /items?lat=..&lon=..`

## Performance harness
- Add a simple load test script (k6 or Gatling) for:
  - cache hit latency
  - cache miss latency

## Tests
- Unit tests for grid key function
- Integration tests with Testcontainers Redis
