package com.example.localdelivery.redisgeo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

@Repository
public class WarehouseGeoRepository {
    private static final String WAREHOUSE_GEO_KEY = "warehouses:geo";
    private static final String WAREHOUSE_HASH_PREFIX = "warehouse:";

    private final RedisTemplate<String, String> redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public WarehouseGeoRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    @PostConstruct
    public void seedDemoWarehouses() {
        // NYC-ish demo data
        save(new Warehouse("wh1", "Downtown Warehouse", 40.7128, -74.0060));
        save(new Warehouse("wh2", "Midtown Warehouse", 40.7580, -73.9855));
        save(new Warehouse("wh3", "Brooklyn Warehouse", 40.6782, -73.9442));
        save(new Warehouse("wh4", "Queens Warehouse", 40.7282, -73.7949));
        save(new Warehouse("wh5", "Jersey City Warehouse", 40.7178, -74.0431));
    }

    public void save(Warehouse warehouse) {
        geoOps.add(
                WAREHOUSE_GEO_KEY,
                new Point(warehouse.longitude(), warehouse.latitude()),
                warehouse.id()
        );

        String key = WAREHOUSE_HASH_PREFIX + warehouse.id();
        redisTemplate.opsForHash().put(key, "name", warehouse.name());
        redisTemplate.opsForHash().put(key, "lat", String.valueOf(warehouse.latitude()));
        redisTemplate.opsForHash().put(key, "lon", String.valueOf(warehouse.longitude()));
    }

    public List<Warehouse> findAll() {
        // Redis GEO doesn't support listing all members directly; for demo simplicity, we rely on the seeded IDs.
        return List.of(
                load("wh1"),
                load("wh2"),
                load("wh3"),
                load("wh4"),
                load("wh5")
        ).stream().filter(Objects::nonNull).toList();
    }

    public List<WarehouseDistance> findNearby(double latitude, double longitude, long radiusMeters) {
        Circle circle = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
            WAREHOUSE_GEO_KEY,
            circle,
            RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance()
        );
        if (results == null) {
            return List.of();
        }

        List<WarehouseDistance> out = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
            String id = result.getContent().getName();
            Warehouse warehouse = load(id);
            if (warehouse != null) {
                Double distanceMeters = result.getDistance() == null ? null : result.getDistance().getValue() * 1000.0;
                out.add(new WarehouseDistance(warehouse, distanceMeters));
            }
        }

        // Redis might not include distances depending on command; keep ordering stable.
        out.sort(Comparator.comparingDouble(wd -> {
            Double meters = wd.distanceMeters();
            return meters == null ? Double.POSITIVE_INFINITY : meters;
        }));
        return out;
    }

    public List<WarehouseDistance> findNearest(double latitude, double longitude, int limit) {
        // Use a wide radius and then take top N; for demo purposes.
        List<WarehouseDistance> candidates = findNearby(latitude, longitude, 50_000);
        return candidates.stream().limit(limit).toList();
    }

    private Warehouse load(String warehouseId) {
        String key = WAREHOUSE_HASH_PREFIX + warehouseId;
        Object nameObj = redisTemplate.opsForHash().get(key, "name");
        Object latObj = redisTemplate.opsForHash().get(key, "lat");
        Object lonObj = redisTemplate.opsForHash().get(key, "lon");
        if (nameObj == null || latObj == null || lonObj == null) {
            return null;
        }
        return new Warehouse(
                warehouseId,
                nameObj.toString(),
                Double.parseDouble(latObj.toString()),
                Double.parseDouble(lonObj.toString())
        );
    }

    public record WarehouseDistance(Warehouse warehouse, Double distanceMeters) {}
}
