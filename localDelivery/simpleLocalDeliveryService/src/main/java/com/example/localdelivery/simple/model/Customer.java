package com.example.localdelivery.simple.model;

import java.util.UUID;

public record Customer(UUID id, String name, double latitude, double longitude) {}
