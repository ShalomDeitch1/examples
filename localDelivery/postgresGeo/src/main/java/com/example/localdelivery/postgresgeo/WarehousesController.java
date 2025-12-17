package com.example.localdelivery.postgresgeo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouses")
public class WarehousesController {

    private final WarehouseRepository repository;

    public WarehousesController(WarehouseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Warehouse> all() {
        return repository.findAll();
    }

    @GetMapping("/nearby")
    public List<Warehouse> nearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam long radiusMeters,
            @RequestParam(name = "twoPhase", defaultValue = "false") boolean twoPhase
    ) {
        if (twoPhase) {
            return repository.findWithinRadiusTwoPhase(lat, lon, radiusMeters);
        }
        return repository.findWithinRadius(lat, lon, radiusMeters);
    }

    @GetMapping("/nearest")
    public List<Warehouse> nearest(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam int limit
    ) {
        return repository.findNearest(lat, lon, limit);
    }
}
