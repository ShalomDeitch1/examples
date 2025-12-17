CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS warehouses (
    warehouse_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    location geography(Point, 4326) NOT NULL,
    grid_prefix TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_warehouses_location_gist ON warehouses USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_warehouses_grid_prefix ON warehouses (grid_prefix);
