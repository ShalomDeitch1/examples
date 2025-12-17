package com.example.localdelivery.simple;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        OrderStatus status,
        Instant createdAt,
        List<OrderLine> lines
) {}
