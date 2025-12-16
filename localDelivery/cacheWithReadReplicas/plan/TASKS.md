# Tasks — cacheWithReadReplicas

## Infra
- Docker compose with:
  - Postgres primary
  - Postgres replica(s)
  - Redis

## Cache key strategy
- Data key:
  - `items:grid:{gridId}:v{version}`
- Version key:
  - `items:grid:{gridId}:version`

## Write invalidation/version bump
- On inventory changes (including reservations), bump relevant grid version keys
- Keep it simple for the example: bump all grids or a small set of demo grids

## Read flow
- Read version first
- Read `items:grid:{gridId}:v{version}`
- On miss, query replica, build response, write cache with TTL

## Tests
- Unit tests for versioned key construction
- Integration tests for cache hit/miss flows
