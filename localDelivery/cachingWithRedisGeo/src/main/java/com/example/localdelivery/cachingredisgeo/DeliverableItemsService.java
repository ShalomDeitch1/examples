package com.example.localdelivery.cachingredisgeo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DeliverableItemsService {
    private static final String CACHE_KEY_PREFIX = "deliverable-items:grid:";
    private static final long CACHE_TTL_MINUTES = 15;
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600; // 1 hour
    private static final double SEARCH_RADIUS_KM = 10.0;

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final TravelTimeService travelTimeService;
    

    public DeliverableItemsService(
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            TravelTimeService travelTimeService) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.travelTimeService = travelTimeService;
    }

    @Cacheable(value = "deliverable-items", key = "'deliverable-items:grid:' + T(com.example.localdelivery.cachingredisgeo.GridKey).compute(#latitude,#longitude)")
    public List<DeliverableItem> getDeliverableItems(double latitude, double longitude) {
        return computeDeliverableItems(latitude, longitude);
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
