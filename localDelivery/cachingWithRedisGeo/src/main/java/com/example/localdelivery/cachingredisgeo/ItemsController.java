package com.example.localdelivery.cachingredisgeo;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemsController {
    private final DeliverableItemsService deliverableItemsService;

    public ItemsController(DeliverableItemsService deliverableItemsService) {
        this.deliverableItemsService = deliverableItemsService;
    }

    @GetMapping
    public List<DeliverableItem> getDeliverableItems(
            @RequestParam double lat,
            @RequestParam double lon) {
        return deliverableItemsService.getDeliverableItems(lat, lon);
    }
}
