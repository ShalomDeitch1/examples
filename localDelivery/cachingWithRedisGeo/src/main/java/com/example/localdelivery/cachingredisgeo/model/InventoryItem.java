package com.example.localdelivery.cachingredisgeo.model;

public record InventoryItem(
    String itemId,
    String name,
    String warehouseId,
    int quantity
) {}
