package com.example.localdelivery.simple.model;

import java.util.UUID;

public record DeliverableItem(
        UUID itemId,
        String name,
        UUID warehouseId,
        int travelTimeSeconds
) {}
