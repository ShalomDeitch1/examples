package com.example.localdelivery.cachewithreplicas.model;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull Boolean success) {}
