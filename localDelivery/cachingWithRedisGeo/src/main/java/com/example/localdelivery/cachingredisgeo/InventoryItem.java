package com.example.localdelivery.cachingredisgeo;

public record InventoryItem(
    String itemId,
    String name,
    String warehouseId,
    int quantity
) {}
