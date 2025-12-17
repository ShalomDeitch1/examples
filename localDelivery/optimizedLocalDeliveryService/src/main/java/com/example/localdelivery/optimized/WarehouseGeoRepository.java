package com.example.localdelivery.optimized;

import jakarta.annotation.PostConstruct;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class WarehouseGeoRepository {
    private static final String GEO_KEY = "warehouses:geo";

    private final RedisTemplate<String, String> redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public WarehouseGeoRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    public void clearAndIndex(List<Models.Warehouse> warehouses) {
        redisTemplate.delete(GEO_KEY);
        for (Models.Warehouse w : warehouses) {
            geoOps.add(GEO_KEY, new Point(w.longitude(), w.latitude()), w.id().toString());
        }
    }

    public List<UUID> findNearbyWarehouseIds(double latitude, double longitude, double radiusKm, int limit) {
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS));
        var results = geoOps.radius(GEO_KEY, circle);
        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .limit(limit)
                .map(r -> UUID.fromString(r.getContent().getName()))
                .toList();
    }
}
