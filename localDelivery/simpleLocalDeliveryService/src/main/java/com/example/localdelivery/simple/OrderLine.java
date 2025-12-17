package com.example.localdelivery.simple;

import java.util.UUID;

public record OrderLine(UUID orderId, UUID itemId, UUID warehouseId, int qty) {}
