package com.example.localdelivery.redisgeo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouses")
public class WarehousesController {

    private final WarehouseGeoRepository repository;

    public WarehousesController(WarehouseGeoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Warehouse> getAll() {
        return repository.findAll();
    }

    @GetMapping("/nearby")
    public List<WarehouseGeoRepository.WarehouseDistance> nearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam long radiusMeters
    ) {
        return repository.findNearby(lat, lon, radiusMeters);
    }

    @GetMapping("/nearest")
    public List<WarehouseGeoRepository.WarehouseDistance> nearest(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam int limit
    ) {
        return repository.findNearest(lat, lon, limit);
    }
}
