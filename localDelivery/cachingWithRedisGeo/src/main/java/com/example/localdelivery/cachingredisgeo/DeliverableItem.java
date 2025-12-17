package com.example.localdelivery.cachingredisgeo;

public record DeliverableItem(
    String itemId,
    String name,
    String warehouseId,
    int travelTimeSeconds
) {}
