package com.example.localdelivery.postgresgeo;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final WarehouseRepository repository;

    public DemoDataLoader(WarehouseRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.isEmpty()) {
            return;
        }

        // NYC-ish demo data (same as redisGeo).
        List<Warehouse> warehouses = List.of(
                newWarehouse("Downtown Warehouse", 40.7128, -74.0060),
                newWarehouse("Midtown Warehouse", 40.7580, -73.9855),
                newWarehouse("Brooklyn Warehouse", 40.6782, -73.9442),
                newWarehouse("Queens Warehouse", 40.7282, -73.7949),
                newWarehouse("Jersey City Warehouse", 40.7178, -74.0431),
                newWarehouse("Harlem Warehouse", 40.8116, -73.9465),
                newWarehouse("Hoboken Warehouse", 40.7433, -74.0288),
                newWarehouse("Long Island City Warehouse", 40.7440, -73.9489),
                newWarehouse("Williamsburg Warehouse", 40.7081, -73.9571),
                newWarehouse("Lower East Side Warehouse", 40.7150, -73.9843)
        );

        for (Warehouse w : warehouses) {
            repository.insert(w);
        }
    }

    private Warehouse newWarehouse(String name, double lat, double lon) {
        return new Warehouse(UUID.randomUUID(), name, lat, lon, GridKey.compute(lat, lon), null);
    }
}
