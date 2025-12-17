package com.example.localdelivery.cachingredisgeo;

import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Repository
public class InventoryRepository {
    private final Map<String, List<InventoryItem>> warehouseInventory = new HashMap<>();

    @PostConstruct
    public void initializeData() {
        // Initialize with sample inventory
        warehouseInventory.put("wh1", List.of(
            new InventoryItem("item1", "Milk", "wh1", 50),
            new InventoryItem("item2", "Bread", "wh1", 100),
            new InventoryItem("item3", "Eggs", "wh1", 75)
        ));
        
        warehouseInventory.put("wh2", List.of(
            new InventoryItem("item1", "Milk", "wh2", 30),
            new InventoryItem("item4", "Cheese", "wh2", 40),
            new InventoryItem("item5", "Butter", "wh2", 60)
        ));
        
        warehouseInventory.put("wh3", List.of(
            new InventoryItem("item2", "Bread", "wh3", 80),
            new InventoryItem("item3", "Eggs", "wh3", 90),
            new InventoryItem("item6", "Yogurt", "wh3", 45)
        ));
    }

    public List<InventoryItem> getInventoryForWarehouse(String warehouseId) {
        return warehouseInventory.getOrDefault(warehouseId, List.of());
    }
}
