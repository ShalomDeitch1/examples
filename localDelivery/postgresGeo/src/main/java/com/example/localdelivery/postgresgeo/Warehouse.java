package com.example.localdelivery.postgresgeo;

import java.util.UUID;

public record Warehouse(
        UUID id,
        String name,
        double latitude,
        double longitude,
        String gridPrefix,
        Double distanceMeters
) {}
