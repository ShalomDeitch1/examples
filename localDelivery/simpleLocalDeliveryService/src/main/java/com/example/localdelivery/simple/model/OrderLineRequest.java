package com.example.localdelivery.simple.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderLineRequest(
        @NotNull UUID itemId,
        @Min(1) int qty
) {}
