package com.example.localdelivery.postgresreplicas.service;

import com.example.localdelivery.postgresreplicas.dao.ReadDao;
import com.example.localdelivery.postgresreplicas.model.Models;
import com.example.localdelivery.postgresreplicas.service.TravelTimeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliverableItemsService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600;

    private final ReadDao readDao;
    private final TravelTimeService travelTimeService;

    public DeliverableItemsService(ReadDao readDao, TravelTimeService travelTimeService) {
        this.readDao = readDao;
        this.travelTimeService = travelTimeService;
    }

    /**
     * Read path: uses the replica datasource via ReadDao.
     */
    public List<Models.DeliverableItem> listDeliverableItems(UUID customerId) {
        Models.Customer customer = readDao.findCustomer(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown customerId"));

        List<Models.Warehouse> warehouses = readDao.findWarehouses();

        Map<UUID, Integer> travelSecondsByWarehouse = new HashMap<>();
        List<UUID> eligibleWarehouseIds = new ArrayList<>();

        for (Models.Warehouse w : warehouses) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), customer.latitude(), customer.longitude());
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
