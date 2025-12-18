package com.example.localdelivery.postgresgeo;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public WarehouseRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = namedParameterJdbcTemplate.getJdbcTemplate();
    }

    public boolean isEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouses", Integer.class);
        return count == null || count == 0;
    }

    public void insert(Warehouse warehouse) {
        String sql = "INSERT INTO warehouses(warehouse_id, name, location, grid_prefix) " +
                "VALUES(:id, :name, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :grid)";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", warehouse.id(), java.sql.Types.OTHER)
                .addValue("name", warehouse.name())
                .addValue("lon", warehouse.longitude())
                .addValue("lat", warehouse.latitude())
                .addValue("grid", warehouse.gridPrefix());

        namedParameterJdbcTemplate.update(sql, params);
    }

    public List<Warehouse> findAll() {
        return jdbcTemplate.query(
                "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix FROM warehouses ORDER BY name",
                warehouseRowMapper(false)
        );
    }

    public List<Warehouse> findWithinRadius(double latitude, double longitude, long radiusMeters) {
        String sql = "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                "ST_Distance(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance_meters " +
                "FROM warehouses " +
                "WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
                "ORDER BY distance_meters ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lon", longitude)
                .addValue("lat", latitude)
                .addValue("radius", radiusMeters);

        return namedParameterJdbcTemplate.query(sql, params, warehouseRowMapper(true));
    }

    public List<Warehouse> findNearest(double latitude, double longitude, int limit) {
        String sql = "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                "ST_Distance(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance_meters " +
                "FROM warehouses " +
                "ORDER BY distance_meters ASC " +
                "LIMIT :limit";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lon", longitude)
                .addValue("lat", latitude)
                .addValue("limit", limit);

        return namedParameterJdbcTemplate.query(sql, params, warehouseRowMapper(true));
    }

    /**
     * Two-phase query: narrow by grid keys, then exact PostGIS distance filter.
     */
    public List<Warehouse> findWithinRadiusTwoPhase(double latitude, double longitude, long radiusMeters) {
        String gridId = GridKey.compute(latitude, longitude);
        String[] gridKeys = GridKey.neighborsIncludingSelf(gridId);

        String sql = "SELECT warehouse_id, name, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon, grid_prefix, " +
                "ST_Distance(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance_meters " +
                "FROM warehouses " +
                "WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
                "AND grid_prefix IN (:grids) " +
                "ORDER BY distance_meters ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lon", longitude)
                .addValue("lat", latitude)
                .addValue("radius", radiusMeters)
                .addValue("grids", Arrays.asList(gridKeys));

        return namedParameterJdbcTemplate.query(sql, params, warehouseRowMapper(true));
    }

    private RowMapper<Warehouse> warehouseRowMapper(boolean includeDistance) {
        return (rs, rowNum) -> {
            Double distance = null;
            if (includeDistance) {
                double d = rs.getDouble("distance_meters");
                if (!rs.wasNull()) distance = d;
            }

            return new Warehouse(
                    UUID.fromString(rs.getString("warehouse_id")),
                    rs.getString("name"),
                    rs.getDouble("lat"),
                    rs.getDouble("lon"),
                    rs.getString("grid_prefix"),
                    distance
            );
        };
    }
}
