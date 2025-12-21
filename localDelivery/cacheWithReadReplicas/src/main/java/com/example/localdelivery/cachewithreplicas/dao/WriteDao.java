package com.example.localdelivery.cachewithreplicas.dao;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.localdelivery.cachewithreplicas.model.Models;

@Repository
public class WriteDao {

    private final JdbcTemplate primaryJdbc;

    public WriteDao(@Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbc) {
        this.primaryJdbc = primaryJdbc;
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public boolean reserve(UUID warehouseId, UUID itemId, int qty) {
        int updated = primaryJdbc.update(
                "UPDATE inventory " +
                        "SET available_qty = available_qty - ?, reserved_qty = reserved_qty + ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND available_qty >= ?",
                qty, qty, warehouseId, itemId, qty
        );
        return updated == 1;
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void consumeReservation(UUID warehouseId, UUID itemId, int qty) {
        int updated = primaryJdbc.update(
                "UPDATE inventory SET reserved_qty = reserved_qty - ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND reserved_qty >= ?",
                qty, warehouseId, itemId, qty
        );
        if (updated != 1) {
            throw new IllegalStateException("Reservation not found to consume");
        }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void releaseReservation(UUID warehouseId, UUID itemId, int qty) {
        int updated = primaryJdbc.update(
                "UPDATE inventory SET reserved_qty = reserved_qty - ?, available_qty = available_qty + ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND reserved_qty >= ?",
                qty, qty, warehouseId, itemId, qty
        );
        if (updated != 1) {
            throw new IllegalStateException("Reservation not found to release");
        }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void insertOrder(Models.Order order) {
        primaryJdbc.update(
                "INSERT INTO orders(order_id, customer_id, status, created_at) VALUES(?, ?, ?, ?)",
                order.id(), order.customerId(), order.status().name(), Timestamp.from(order.createdAt())
        );
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void insertOrderLine(Models.OrderLine line) {
        primaryJdbc.update(
                "INSERT INTO order_lines(order_id, item_id, warehouse_id, qty) VALUES(?, ?, ?, ?)",
                line.orderId(), line.itemId(), line.warehouseId(), line.qty()
        );
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public Optional<Models.Order> findOrder(UUID orderId) {
        return primaryJdbc.query(
                "SELECT order_id, customer_id, status, created_at FROM orders WHERE order_id = ?",
                (rs, rowNum) -> new Models.Order(
                        UUID.fromString(rs.getString("order_id")),
                        UUID.fromString(rs.getString("customer_id")),
                        Models.OrderStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()
                ),
                orderId
        ).stream().findFirst();
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public List<Models.OrderLine> findLines(UUID orderId) {
        return primaryJdbc.query(
                "SELECT order_id, item_id, warehouse_id, qty FROM order_lines WHERE order_id = ?",
                (rs, rowNum) -> new Models.OrderLine(
                        UUID.fromString(rs.getString("order_id")),
                        UUID.fromString(rs.getString("item_id")),
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getInt("qty")
                ),
                orderId
        );
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void updateStatus(UUID orderId, Models.OrderStatus status) {
        primaryJdbc.update("UPDATE orders SET status = ? WHERE order_id = ?", status.name(), orderId);
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public Instant now() {
        return Instant.now();
    }
}
