package com.example.localdelivery.optimized.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.localdelivery.optimized.model.Models;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        Models.OrderStatus status,
        Instant createdAt,
        List<Models.OrderLine> lines
) {}
