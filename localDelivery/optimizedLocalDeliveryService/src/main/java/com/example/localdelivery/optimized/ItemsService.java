package com.example.localdelivery.optimized;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ItemsService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600;
    private static final long CACHE_TTL_MINUTES = 15;

    // Demo defaults
    private static final double WAREHOUSE_SEARCH_RADIUS_KM = 10.0;
    private static final int MAX_WAREHOUSES = 50;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheVersionService versionService;
    private final WarehouseGeoRepository geoRepository;
    private final ReadDao readDao;
    private final TravelTimeService travelTimeService;

    public ItemsService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            CacheVersionService versionService,
            WarehouseGeoRepository geoRepository,
            ReadDao readDao,
            TravelTimeService travelTimeService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.versionService = versionService;
        this.geoRepository = geoRepository;
        this.readDao = readDao;
        this.travelTimeService = travelTimeService;
    }

    public List<Models.DeliverableItem> listDeliverableItems(double lat, double lon) {
        String gridId = GridKey.compute(lat, lon);
        long version = versionService.getVersion(gridId);
        String cacheKey = versionService.dataKey(gridId, version);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        List<Models.DeliverableItem> computed = compute(lat, lon);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(computed), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }

        return computed;
    }

    private List<Models.DeliverableItem> compute(double lat, double lon) {
        List<UUID> nearbyIds = geoRepository.findNearbyWarehouseIds(lat, lon, WAREHOUSE_SEARCH_RADIUS_KM, MAX_WAREHOUSES);
        List<Models.Warehouse> warehouses = readDao.findWarehousesByIds(nearbyIds);

        Map<UUID, Integer> travelSecondsByWarehouse = new HashMap<>();
        List<UUID> eligibleWarehouseIds = new ArrayList<>();

        for (Models.Warehouse w : warehouses) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), lat, lon);
            if (seconds <= MAX_TRAVEL_TIME_SECONDS) {
                travelSecondsByWarehouse.put(w.id(), seconds);
                eligibleWarehouseIds.add(w.id());
            }
        }

        List<Models.InventoryRow> inventory = readDao.findAvailableInventoryForWarehouses(eligibleWarehouseIds);

        Map<UUID, Models.DeliverableItem> best = new HashMap<>();
        for (Models.InventoryRow row : inventory) {
            int seconds = travelSecondsByWarehouse.getOrDefault(row.warehouseId(), Integer.MAX_VALUE);
            Models.DeliverableItem existing = best.get(row.itemId());
            if (existing == null || seconds < existing.travelTimeSeconds()) {
                best.put(row.itemId(), new Models.DeliverableItem(row.itemId(), row.itemName(), row.warehouseId(), seconds));
            }
        }

        return best.values().stream()
                .sorted((a, b) -> Integer.compare(a.travelTimeSeconds(), b.travelTimeSeconds()))
                .toList();
    }
}
