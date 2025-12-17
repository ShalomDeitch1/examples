package com.example.localdelivery.postgresreplicas.dao;

import com.example.localdelivery.postgresreplicas.model.Models;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReadDao {

    private final JdbcTemplate replicaJdbc;

    public ReadDao(@Qualifier("replicaJdbcTemplate") JdbcTemplate replicaJdbc) {
        this.replicaJdbc = replicaJdbc;
    }

    @Transactional(readOnly = true, transactionManager = "replicaTransactionManager")
    public Optional<Models.Customer> findCustomer(UUID customerId) {
        return replicaJdbc.query(
                "SELECT customer_id, name, latitude, longitude FROM customers WHERE customer_id = ?",
                (rs, rowNum) -> new Models.Customer(
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ),
                customerId
        ).stream().findFirst();
    }

    @Transactional(readOnly = true, transactionManager = "replicaTransactionManager")
    public List<Models.Warehouse> findWarehouses() {
        return replicaJdbc.query(
                "SELECT warehouse_id, name, latitude, longitude FROM warehouses ORDER BY name",
                (rs, rowNum) -> new Models.Warehouse(
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );
    }

    @Transactional(readOnly = true, transactionManager = "replicaTransactionManager")
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

        return replicaJdbc.query(
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
