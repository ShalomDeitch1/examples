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

@Repository
public class WriteDao {

    private final NamedParameterJdbcTemplate primaryNamedJdbc;

    public WriteDao(@Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbc) {
    this.primaryNamedJdbc = new NamedParameterJdbcTemplate(primaryJdbc.getDataSource());
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public boolean reserve(UUID warehouseId, UUID itemId, int qty) {
    String sql = "UPDATE inventory " +
        "SET available_qty = available_qty - :qty, reserved_qty = reserved_qty + :qty " +
        "WHERE warehouse_id = :warehouseId AND item_id = :itemId AND available_qty >= :qty";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("qty", qty)
        .addValue("warehouseId", warehouseId.toString())
        .addValue("itemId", itemId.toString());

    int updated = primaryNamedJdbc.update(sql, params);
    return updated == 1;
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void consumeReservation(UUID warehouseId, UUID itemId, int qty) {
    String sql = "UPDATE inventory SET reserved_qty = reserved_qty - :qty " +
        "WHERE warehouse_id = :warehouseId AND item_id = :itemId AND reserved_qty >= :qty";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("qty", qty)
        .addValue("warehouseId", warehouseId.toString())
        .addValue("itemId", itemId.toString());

    int updated = primaryNamedJdbc.update(sql, params);
    if (updated != 1) {
        throw new IllegalStateException("Reservation not found to consume");
    }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void releaseReservation(UUID warehouseId, UUID itemId, int qty) {
    String sql = "UPDATE inventory SET reserved_qty = reserved_qty - :qty, available_qty = available_qty + :qty " +
        "WHERE warehouse_id = :warehouseId AND item_id = :itemId AND reserved_qty >= :qty";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("qty", qty)
        .addValue("warehouseId", warehouseId.toString())
        .addValue("itemId", itemId.toString());

    int updated = primaryNamedJdbc.update(sql, params);
    if (updated != 1) {
        throw new IllegalStateException("Reservation not found to release");
    }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void insertOrder(Models.Order order) {
    String sql = "INSERT INTO orders(order_id, customer_id, status, created_at) VALUES(:orderId, :customerId, :status, :createdAt)";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("orderId", order.id().toString())
        .addValue("customerId", order.customerId().toString())
        .addValue("status", order.status().name())
        .addValue("createdAt", java.sql.Timestamp.from(order.createdAt()));

    primaryNamedJdbc.update(sql, params);
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void insertOrderLine(Models.OrderLine line) {
    String sql = "INSERT INTO order_lines(order_id, item_id, warehouse_id, qty) VALUES(:orderId, :itemId, :warehouseId, :qty)";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("orderId", line.orderId().toString())
        .addValue("itemId", line.itemId().toString())
        .addValue("warehouseId", line.warehouseId().toString())
        .addValue("qty", line.qty());

    primaryNamedJdbc.update(sql, params);
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public Optional<Models.Order> findOrder(UUID orderId) {
    String sql = "SELECT order_id, customer_id, status, created_at FROM orders WHERE order_id = :orderId";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("orderId", orderId.toString());

    return primaryNamedJdbc.query(
        sql,
        params,
        (rs, rowNum) -> new Models.Order(
            UUID.fromString(rs.getString("order_id")),
            UUID.fromString(rs.getString("customer_id")),
            Models.OrderStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toInstant()
        )
    ).stream().findFirst();
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public List<Models.OrderLine> findLines(UUID orderId) {
    String sql = "SELECT order_id, item_id, warehouse_id, qty FROM order_lines WHERE order_id = :orderId";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("orderId", orderId.toString());

    return primaryNamedJdbc.query(
        sql,
        params,
        (rs, rowNum) -> new Models.OrderLine(
            UUID.fromString(rs.getString("order_id")),
            UUID.fromString(rs.getString("item_id")),
            UUID.fromString(rs.getString("warehouse_id")),
            rs.getInt("qty")
        )
    );
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void updateStatus(UUID orderId, Models.OrderStatus status) {
    String sql = "UPDATE orders SET status = :status WHERE order_id = :orderId";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("status", status.name())
        .addValue("orderId", orderId.toString());

    primaryNamedJdbc.update(sql, params);
    }
}
