package com.example.localdelivery.cachewithreplicas;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull Boolean success) {}
