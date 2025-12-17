package com.example.localdelivery.optimized;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull Boolean success) {}
