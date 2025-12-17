package com.example.localdelivery.cachewithreplicas;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderLineRequest(
        @NotNull UUID itemId,
        @Min(1) int qty
) {}
