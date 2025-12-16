# Tasks — postgresGeo

## Data model + migrations
- Create a `warehouses` table with:
  - `warehouse_id` (UUID)
  - `name`
  - `location` as `geography(Point, 4326)`
  - (Optional) `grid_prefix` (e.g., geohash prefix or 1km grid key)
- Add indexes:
  - GiST/SP-GiST on `location`
  - (Optional) btree index on `grid_prefix`

## Queries to implement
- Query warehouses within a radius of a customer point (e.g., 3km)
- Query k-nearest warehouses (top N by distance)
- Optional 2-phase query:
  1) filter by `grid_prefix` prefix set
  2) exact distance filter via PostGIS

## API endpoints (minimal)
- `GET /warehouses/nearby?lat=..&lon=..&radiusMeters=..`
- `GET /warehouses/nearest?lat=..&lon=..&limit=..`

## Demo data
- Seed 20–100 warehouses with known coordinates in migration or on startup

## Tests
- Integration tests using Testcontainers Postgres + PostGIS image
- Assertions:
  - warehouses are returned in expected order for nearest query
  - radius query returns only those within radius

## Documentation
- Add example curl commands and sample responses in README
