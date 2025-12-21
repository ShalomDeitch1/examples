package com.example.localdelivery.cachewithreplicas.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        Models.OrderStatus status,
        Instant createdAt,
        List<Models.OrderLine> lines
) {}
