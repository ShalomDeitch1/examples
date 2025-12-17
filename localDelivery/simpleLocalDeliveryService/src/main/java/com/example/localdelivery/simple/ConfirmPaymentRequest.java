package com.example.localdelivery.simple;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull Boolean success) {}
