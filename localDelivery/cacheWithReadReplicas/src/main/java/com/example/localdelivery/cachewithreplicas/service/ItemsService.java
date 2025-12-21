package com.example.localdelivery.cachewithreplicas.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.example.localdelivery.cachewithreplicas.dao.PrimaryReadDao;
import com.example.localdelivery.cachewithreplicas.dao.ReadDao;
import com.example.localdelivery.cachewithreplicas.model.Models;
import com.example.localdelivery.cachewithreplicas.util.GridKey;

@Service
public class ItemsService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600;
    private static final long CACHE_TTL_MINUTES = 15;
    private static final long REPLICA_LAG_GUARD_MILLIS = 3_000;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheVersionService versionService;
    private final ReadDao readDao;
    private final PrimaryReadDao primaryReadDao;
    private final TravelTimeService travelTimeService;

    public ItemsService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            CacheVersionService versionService,
            ReadDao readDao,
            PrimaryReadDao primaryReadDao,
            TravelTimeService travelTimeService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.versionService = versionService;
        this.readDao = readDao;
        this.primaryReadDao = primaryReadDao;
        this.travelTimeService = travelTimeService;
    }

    public List<Models.DeliverableItem> listDeliverableItems(double lat, double lon) {
        String gridId = GridKey.compute(lat, lon);
        long version = versionService.getVersion(gridId);
        String cacheKey = versionService.dataKey(gridId, version);

        String cached = redisTemplate.opsForValue().get(Objects.requireNonNull(cacheKey));
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (Exception ignored) {
                // fall through to recompute
            }
        }

        List<Models.DeliverableItem> computed = compute(lat, lon, shouldBypassReplica(gridId));

        try {
            redisTemplate.opsForValue().set(
                    Objects.requireNonNull(cacheKey),
                    Objects.requireNonNull(objectMapper.writeValueAsString(computed)),
                    CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception ignored) {
            // keep API available even if cache write fails
        }

        return computed;
    }

    private boolean shouldBypassReplica(String gridId) {
        long lastWriteMs;
        try {
            lastWriteMs = versionService.getLastWriteMillis(gridId);
        } catch (Exception ignored) {
            return false;
        }
        return lastWriteMs > 0 && (System.currentTimeMillis() - lastWriteMs) < REPLICA_LAG_GUARD_MILLIS;
    }

    private List<Models.DeliverableItem> compute(double lat, double lon, boolean bypassReplica) {
        List<Models.Warehouse> warehouses = bypassReplica
                ? primaryReadDao.findWarehouses()
                : readDao.findWarehouses();

        Map<UUID, Integer> travelSecondsByWarehouse = new HashMap<>();
        List<UUID> eligibleWarehouseIds = new ArrayList<>();

        for (Models.Warehouse w : warehouses) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), lat, lon);
            if (seconds <= MAX_TRAVEL_TIME_SECONDS) {
                travelSecondsByWarehouse.put(w.id(), seconds);
                eligibleWarehouseIds.add(w.id());
            }
        }

        List<Models.InventoryRow> inventory = bypassReplica
            ? primaryReadDao.findAvailableInventoryForWarehouses(eligibleWarehouseIds)
            : readDao.findAvailableInventoryForWarehouses(eligibleWarehouseIds);

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
