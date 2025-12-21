package com.example.localdelivery.cachingredisgeo.repository;

import com.example.localdelivery.cachingredisgeo.model.Warehouse;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Metrics;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class WarehouseRepository {
    private static final String WAREHOUSE_GEO_KEY = "warehouses:geo";
    private final RedisTemplate<String, String> redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public WarehouseRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    @PostConstruct
    public void initializeData() {
        // Initialize with some sample warehouses
        saveWarehouse(new Warehouse("wh1", "Downtown Warehouse", 40.7128, -74.0060));
        saveWarehouse(new Warehouse("wh2", "Midtown Warehouse", 40.7580, -73.9855));
        saveWarehouse(new Warehouse("wh3", "Brooklyn Warehouse", 40.6782, -73.9442));
    }

    public void saveWarehouse(Warehouse warehouse) {
        geoOps.add(WAREHOUSE_GEO_KEY, 
                   new Point(warehouse.longitude(), warehouse.latitude()), 
                   warehouse.id());
        
        // Store warehouse details separately
        redisTemplate.opsForHash().put("warehouse:" + warehouse.id(), "name", warehouse.name());
        redisTemplate.opsForHash().put("warehouse:" + warehouse.id(), "lat", String.valueOf(warehouse.latitude()));
        redisTemplate.opsForHash().put("warehouse:" + warehouse.id(), "lon", String.valueOf(warehouse.longitude()));
    }

    public List<Warehouse> findNearby(double latitude, double longitude, double radiusKm) {
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS));
        
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
            geoOps.radius(WAREHOUSE_GEO_KEY, circle);
        
        if (results == null) {
            return List.of();
        }
        
        return results.getContent().stream()
            .map(result -> {
                String id = result.getContent().getName();
                String name = (String) redisTemplate.opsForHash().get("warehouse:" + id, "name");
                double lat = Double.parseDouble((String) redisTemplate.opsForHash().get("warehouse:" + id, "lat"));
                double lon = Double.parseDouble((String) redisTemplate.opsForHash().get("warehouse:" + id, "lon"));
                return new Warehouse(id, name, lat, lon);
            })
            .collect(Collectors.toList());
    }
}
