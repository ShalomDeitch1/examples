package com.example.localdelivery.cachingredisgeo.model;

public record DeliverableItem(
    String itemId,
    String name,
    String warehouseId,
    int travelTimeSeconds
) {}
