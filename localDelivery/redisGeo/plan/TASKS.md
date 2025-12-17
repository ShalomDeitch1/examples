# Tasks — redisGeo

## Key design
- Choose a key for warehouse geo index, e.g.:
  - `warehouses:geo` (single set)
  - or `warehouses:geo:{region}` (prefixable by region)

## Data loading
- Load a set of warehouses:
  - `GEOADD warehouses:geo <lon> <lat> <warehouseId>`

## Queries
- “Nearby warehouses” given customer coordinates and radius
- “Top N nearest warehouses”

## API endpoints
- `GET /warehouses/nearby?lat=..&lon=..&radiusMeters=..`
- `GET /warehouses/nearest?lat=..&lon=..&limit=..`
- 'Get /warehouses'


## Tests
- Integration test using Testcontainers Redis
- Verify response ordering and radius filtering

## Documentation
- Add example curl commands and sample responses in README
