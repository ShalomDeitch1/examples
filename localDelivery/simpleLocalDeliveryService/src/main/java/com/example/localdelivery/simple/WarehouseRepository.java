package com.example.localdelivery.simple;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseRepository {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Warehouse> findAll() {
        return jdbcTemplate.query(
                "SELECT warehouse_id, name, latitude, longitude FROM warehouses ORDER BY name",
                (rs, rowNum) -> new Warehouse(
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );
    }
}
