package com.example.localdelivery.simple;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InventoryRow> findAvailableItemsForWarehouses(List<UUID> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return List.of();
        }

        String placeholders = warehouseIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");

        String sql =
                "SELECT inv.warehouse_id, inv.item_id, inv.available_qty, i.name AS item_name " +
                        "FROM inventory inv " +
                        "JOIN items i ON i.item_id = inv.item_id " +
                        "WHERE inv.available_qty > 0 AND inv.warehouse_id IN (" + placeholders + ")";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new InventoryRow(
                        UUID.fromString(rs.getString("warehouse_id")),
                        UUID.fromString(rs.getString("item_id")),
                        rs.getString("item_name"),
                        rs.getInt("available_qty")
                ),
                warehouseIds.toArray()
        );
    }

    /**
     * Atomic reservation.
     *
     * This uses a conditional UPDATE so inventory never goes negative,
     * even under heavy concurrency.
     */
    public boolean reserve(UUID warehouseId, UUID itemId, int qty) {
        int updated = jdbcTemplate.update(
                "UPDATE inventory " +
                        "SET available_qty = available_qty - ?, reserved_qty = reserved_qty + ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND available_qty >= ?",
                qty, qty, warehouseId, itemId, qty
        );
        return updated == 1;
    }

    public void consumeReservation(UUID warehouseId, UUID itemId, int qty) {
        int updated = jdbcTemplate.update(
                "UPDATE inventory " +
                        "SET reserved_qty = reserved_qty - ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND reserved_qty >= ?",
                qty, warehouseId, itemId, qty
        );
        if (updated != 1) {
            throw new IllegalStateException("Reservation not found to consume");
        }
    }

    public void releaseReservation(UUID warehouseId, UUID itemId, int qty) {
        int updated = jdbcTemplate.update(
                "UPDATE inventory " +
                        "SET reserved_qty = reserved_qty - ?, available_qty = available_qty + ? " +
                        "WHERE warehouse_id = ? AND item_id = ? AND reserved_qty >= ?",
                qty, qty, warehouseId, itemId, qty
        );
        if (updated != 1) {
            throw new IllegalStateException("Reservation not found to release");
        }
    }

    public record InventoryRow(UUID warehouseId, UUID itemId, String itemName, int availableQty) {}
}
