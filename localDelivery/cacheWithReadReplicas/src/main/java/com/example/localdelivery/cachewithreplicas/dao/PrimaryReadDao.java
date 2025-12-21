package com.example.localdelivery.cachewithreplicas.dao;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.example.localdelivery.cachewithreplicas.model.Models;

@Repository
public class PrimaryReadDao {

    private final JdbcTemplate primaryJdbc;

    public PrimaryReadDao(@Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbc) {
        this.primaryJdbc = primaryJdbc;
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public List<Models.Warehouse> findWarehouses() {
        return primaryJdbc.query(
                "SELECT warehouse_id, name, latitude, longitude FROM warehouses ORDER BY name",
                (rs, rowNum) -> new Models.Warehouse(
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public List<Models.InventoryRow> findAvailableInventoryForWarehouses(List<UUID> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return List.of();
        }

        String placeholders = warehouseIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        String sql =
                "SELECT inv.warehouse_id, inv.item_id, inv.available_qty, i.name AS item_name " +
                        "FROM inventory inv " +
                        "JOIN items i ON i.item_id = inv.item_id " +
                        "WHERE inv.available_qty > 0 AND inv.warehouse_id IN (" + placeholders + ")";

        return primaryJdbc.query(
                sql,
                (rs, rowNum) -> new Models.InventoryRow(
                        UUID.fromString(rs.getString("warehouse_id")),
                        UUID.fromString(rs.getString("item_id")),
                        rs.getString("item_name"),
                        rs.getInt("available_qty")
                ),
                warehouseIds.toArray()
        );
    }
}
