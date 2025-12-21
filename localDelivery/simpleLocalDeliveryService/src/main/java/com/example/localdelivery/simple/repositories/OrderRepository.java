package com.example.localdelivery.simple.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.localdelivery.simple.model.Order;
import com.example.localdelivery.simple.model.OrderLine;
import com.example.localdelivery.simple.model.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertOrder(Order order) {
        jdbcTemplate.update(
                "INSERT INTO orders(order_id, customer_id, status) VALUES(?, ?, ?)",
                order.id(),
                order.customerId(),
                order.status().name()
        );
    }

    public void insertOrderLine(OrderLine line) {
        jdbcTemplate.update(
                "INSERT INTO order_lines(order_id, item_id, warehouse_id, qty) VALUES(?, ?, ?, ?)",
                line.orderId(), line.itemId(), line.warehouseId(), line.qty()
        );
    }

    public Optional<Order> findById(UUID orderId) {
        return jdbcTemplate.query(
                "SELECT order_id, customer_id, status, created_at FROM orders WHERE order_id = ?",
                (rs, rowNum) -> new Order(
                        UUID.fromString(rs.getString("order_id")),
                        UUID.fromString(rs.getString("customer_id")),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()
                ),
                orderId
        ).stream().findFirst();
    }

    public List<OrderLine> findLines(UUID orderId) {
        return jdbcTemplate.query(
                "SELECT order_id, item_id, warehouse_id, qty FROM order_lines WHERE order_id = ?",
                (rs, rowNum) -> new OrderLine(
                        UUID.fromString(rs.getString("order_id")),
                        UUID.fromString(rs.getString("item_id")),
                        UUID.fromString(rs.getString("warehouse_id")),
                        rs.getInt("qty")
                ),
                orderId
        );
    }

    public void updateStatus(UUID orderId, OrderStatus status) {
        jdbcTemplate.update(
                "UPDATE orders SET status = ? WHERE order_id = ?",
                status.name(), orderId
        );
    }
}
