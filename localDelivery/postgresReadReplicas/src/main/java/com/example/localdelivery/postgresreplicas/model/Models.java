package com.example.localdelivery.postgresreplicas.model;

import java.time.Instant;
import java.util.UUID;

public class Models {
    public record Customer(UUID id, String name, double latitude, double longitude) {}

    public record Warehouse(UUID id, String name, double latitude, double longitude) {}

    public record DeliverableItem(UUID itemId, String name, UUID warehouseId, int travelTimeSeconds) {}

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAID,
        PAYMENT_FAILED
    }

    public record Order(UUID id, UUID customerId, OrderStatus status, Instant createdAt) {}

    public record OrderLine(UUID orderId, UUID itemId, UUID warehouseId, int qty) {}

    public record InventoryRow(UUID warehouseId, UUID itemId, String itemName, int availableQty) {}

    private Models() {}
}
