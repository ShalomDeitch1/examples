package com.example.localdelivery.simple;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliverableItemsService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600; // 1 hour

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final TravelTimeService travelTimeService;

    public DeliverableItemsService(
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            TravelTimeService travelTimeService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.travelTimeService = travelTimeService;
    }

    public List<DeliverableItem> listDeliverableItems(double customerLat, double customerLon) {
        List<Warehouse> allWarehouses = warehouseRepository.findAll();

        // Travel time per warehouse
        Map<UUID, Integer> travelSeconds = new HashMap<>();
        List<UUID> eligibleWarehouseIds = new ArrayList<>();
        for (Warehouse w : allWarehouses) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), customerLat, customerLon);
            if (seconds <= MAX_TRAVEL_TIME_SECONDS) {
                travelSeconds.put(w.id(), seconds);
                eligibleWarehouseIds.add(w.id());
            }
        }

        // If no warehouses can deliver in time, nothing is deliverable.
        if (eligibleWarehouseIds.isEmpty()) {
            return List.of();
        }

        List<InventoryRepository.InventoryRow> rows = inventoryRepository.findAvailableItemsForWarehouses(eligibleWarehouseIds);

        // Pick the fastest warehouse for each item.
        Map<UUID, DeliverableItem> best = new HashMap<>();
        for (InventoryRepository.InventoryRow row : rows) {
            int seconds = travelSeconds.getOrDefault(row.warehouseId(), Integer.MAX_VALUE);
            DeliverableItem existing = best.get(row.itemId());
            if (existing == null || seconds < existing.travelTimeSeconds()) {
                best.put(row.itemId(), new DeliverableItem(row.itemId(), row.itemName(), row.warehouseId(), seconds));
            }
        }

        return best.values().stream()
                .sorted((a, b) -> Integer.compare(a.travelTimeSeconds(), b.travelTimeSeconds()))
                .toList();
    }
}
