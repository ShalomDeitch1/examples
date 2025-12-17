package com.example.localdelivery.simple;

import java.util.UUID;

public record Warehouse(UUID id, String name, double latitude, double longitude) {}
