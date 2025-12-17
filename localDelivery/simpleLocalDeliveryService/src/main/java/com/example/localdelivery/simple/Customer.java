package com.example.localdelivery.simple;

import java.util.UUID;

public record Customer(UUID id, String name, double latitude, double longitude) {}
