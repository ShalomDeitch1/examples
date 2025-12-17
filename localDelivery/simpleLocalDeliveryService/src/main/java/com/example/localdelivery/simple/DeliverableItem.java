package com.example.localdelivery.simple;

import java.util.UUID;

public record DeliverableItem(
        UUID itemId,
        String name,
        UUID warehouseId,
        int travelTimeSeconds
) {}
