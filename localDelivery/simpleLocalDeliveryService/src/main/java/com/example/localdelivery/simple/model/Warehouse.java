package com.example.localdelivery.simple.model;

import java.util.UUID;

public record Warehouse(UUID id, String name, double latitude, double longitude) {}
