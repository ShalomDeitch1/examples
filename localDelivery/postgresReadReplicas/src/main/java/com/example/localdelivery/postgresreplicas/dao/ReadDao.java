package com.example.localdelivery.postgresreplicas.dao;

import com.example.localdelivery.postgresreplicas.model.Models;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ReadDao {

    private final NamedParameterJdbcTemplate replicaNamedJdbc;

    public ReadDao(@Qualifier("replicaJdbcTemplate") JdbcTemplate replicaJdbc) {
        this.replicaNamedJdbc = new NamedParameterJdbcTemplate(replicaJdbc.getDataSource());
    }

    @Transactional(readOnly = true, transactionManager = "replicaTransactionManager")
    public Optional<Models.Customer> findCustomer(UUID customerId) {
        String sql = "SELECT customer_id, name, latitude, longitude FROM customers WHERE customer_id = :customerId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("customerId", customerId);

        return replicaNamedJdbc.query(
                sql,
                params,
                (rs, rowNum) -> new Models.Customer(
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        ).stream().findFirst();
    }

    @Transactional(readOnly = true, transactionManager = "replicaTransactionManager")
    public List<Models.Warehouse> findWarehouses() {
        return replicaNamedJdbc.getJdbcTemplate().query(
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

        String sql =
                "SELECT inv.warehouse_id, inv.item_id, inv.available_qty, i.name AS item_name " +
                        "FROM inventory inv " +
                        "JOIN items i ON i.item_id = inv.item_id " +
                        "WHERE inv.available_qty > 0 AND inv.warehouse_id IN (:warehouseIds)";

        List<String> idStrings = warehouseIds.stream().map(UUID::toString).collect(Collectors.toList());
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("warehouseIds", idStrings);

        return replicaNamedJdbc.query(
                sql,
                params,
                (rs, rowNum) -> new Models.InventoryRow(
                        UUID.fromString(rs.getString("warehouse_id")),
                        UUID.fromString(rs.getString("item_id")),
                        rs.getString("item_name"),
                        rs.getInt("available_qty")
                )
        );
    }
}
