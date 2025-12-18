package com.example.localdelivery.simple.model;

import java.util.UUID;

public record OrderLine(UUID orderId, UUID itemId, UUID warehouseId, int qty) {}
