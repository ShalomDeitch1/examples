package com.example.localdelivery.simple;

import java.time.Instant;
import java.util.UUID;

public record Order(UUID id, UUID customerId, OrderStatus status, Instant createdAt) {}
