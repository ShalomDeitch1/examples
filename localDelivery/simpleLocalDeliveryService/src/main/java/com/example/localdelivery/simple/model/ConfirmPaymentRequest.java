package com.example.localdelivery.simple.model;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull Boolean success) {}
