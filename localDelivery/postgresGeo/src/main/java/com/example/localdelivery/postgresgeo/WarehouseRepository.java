package com.example.localdelivery.postgresgeo;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseRepository {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouses", Integer.class);
        return count == null || count == 0;
    }

    public void insert(Warehouse warehouse) {
        jdbcTemplate.update(
                "INSERT INTO warehouses(warehouse_id, name, location, grid_prefix) VALUES(?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)",
                warehouse.id(),
                warehouse.name(),
                warehouse.longitude(),
                warehouse.latitude(),
                warehouse.gridPrefix()
        );
    }

    public List<Warehouse> findAll() {
        return jdbcTemplate.query(
                "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix FROM warehouses ORDER BY name",
                warehouseRowMapper(false)
        );
    }

    public List<Warehouse> findWithinRadius(double latitude, double longitude, long radiusMeters) {
        return jdbcTemplate.query(
                "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                        "ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_meters " +
                        "FROM warehouses " +
                        "WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?) " +
                        "ORDER BY distance_meters ASC",
                warehouseRowMapper(true),
                longitude, latitude,
                longitude, latitude, radiusMeters
        );
    }

    public List<Warehouse> findNearest(double latitude, double longitude, int limit) {
        return jdbcTemplate.query(
                "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                        "ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_meters " +
                        "FROM warehouses " +
                        "ORDER BY distance_meters ASC " +
                        "LIMIT ?",
                warehouseRowMapper(true),
                longitude, latitude, limit
        );
    }

    /**
     * A very simple two-phase query:
     * 1) narrow by a small set of neighboring grid keys
     * 2) do exact distance filtering via PostGIS.
     */
    public List<Warehouse> findWithinRadiusTwoPhase(double latitude, double longitude, long radiusMeters) {
        String gridId = GridKey.compute(latitude, longitude);
        String[] gridKeys = GridKey.neighborsIncludingSelf(gridId);

        // Build IN (?, ?, ...)
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < gridKeys.length; i++) {
            if (i > 0) in.append(",");
            in.append("?");
        }

        String sql =
                "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                        "ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_meters " +
                        "FROM warehouses " +
                        "WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?) " +
                        "AND grid_prefix IN (" + in + ") " +
                        "ORDER BY distance_meters ASC";

        // Note: args contains lon,lat,radius,gridKeys... but query expects lon,lat,lon,lat,radius,gridKeys...
        // Keep it explicit for readability.
        Object[] finalArgs = new Object[5 + gridKeys.length];
        finalArgs[0] = longitude;
        finalArgs[1] = latitude;
        finalArgs[2] = longitude;
        finalArgs[3] = latitude;
        finalArgs[4] = radiusMeters;
        System.arraycopy(gridKeys, 0, finalArgs, 5, gridKeys.length);

        return jdbcTemplate.query(sql, warehouseRowMapper(true), finalArgs);
    }

    private RowMapper<Warehouse> warehouseRowMapper(boolean includeDistance) {
        return (rs, rowNum) -> new Warehouse(
                UUID.fromString(rs.getString("warehouse_id")),
                rs.getString("name"),
                rs.getDouble("lat"),
                rs.getDouble("lon"),
                rs.getString("grid_prefix"),
                includeDistance ? rs.getDouble("distance_meters") : null
        );
    }
}
