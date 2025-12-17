package com.example.localdelivery.cachingredisgeo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DeliverableItemsService {
    private static final String CACHE_KEY_PREFIX = "deliverable-items:grid:";
    private static final long CACHE_TTL_MINUTES = 15;
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600; // 1 hour
    private static final double SEARCH_RADIUS_KM = 10.0;

    private final RedisTemplate<String, String> redisTemplate;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final TravelTimeService travelTimeService;
    private final ObjectMapper objectMapper;

    public DeliverableItemsService(
            RedisTemplate<String, String> redisTemplate,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            TravelTimeService travelTimeService,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.travelTimeService = travelTimeService;
        this.objectMapper = objectMapper;
    }

    public List<DeliverableItem> getDeliverableItems(double latitude, double longitude) {
        String gridId = GridKey.compute(latitude, longitude);
        String cacheKey = CACHE_KEY_PREFIX + gridId;

        // Try cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return Arrays.asList(objectMapper.readValue(cached, DeliverableItem[].class));
            } catch (JsonProcessingException e) {
                // If deserialization fails, recompute
            }
        }

        // Cache miss - compute deliverable items
        List<DeliverableItem> items = computeDeliverableItems(latitude, longitude);

        // Store in cache
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            // Log error but continue
        }

        return items;
    }

    private List<DeliverableItem> computeDeliverableItems(double latitude, double longitude) {
        // Find nearby warehouses
        List<Warehouse> nearbyWarehouses = warehouseRepository.findNearby(latitude, longitude, SEARCH_RADIUS_KM);

        List<DeliverableItem> deliverableItems = new ArrayList<>();
        Set<String> seenItems = new HashSet<>();

        for (Warehouse warehouse : nearbyWarehouses) {
            int travelTime = travelTimeService.estimateTravelTime(
                warehouse.latitude(), warehouse.longitude(), latitude, longitude
            );

            // Only include if within 1 hour delivery
            if (travelTime <= MAX_TRAVEL_TIME_SECONDS) {
                List<InventoryItem> inventory = inventoryRepository.getInventoryForWarehouse(warehouse.id());
                
                for (InventoryItem item : inventory) {
                    if (item.quantity() > 0 && !seenItems.contains(item.itemId())) {
                        deliverableItems.add(new DeliverableItem(
                            item.itemId(),
                            item.name(),
                            warehouse.id(),
                            travelTime
                        ));
                        seenItems.add(item.itemId());
                    }
                }
            }
        }

        return deliverableItems;
    }
}
