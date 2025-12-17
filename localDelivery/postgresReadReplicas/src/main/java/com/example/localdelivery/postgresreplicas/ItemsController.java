package com.example.localdelivery.postgresreplicas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemsController {

    private final DeliverableItemsService deliverableItemsService;

    public ItemsController(DeliverableItemsService deliverableItemsService) {
        this.deliverableItemsService = deliverableItemsService;
    }

    @GetMapping
    public List<Models.DeliverableItem> list(@RequestParam UUID customerId) {
        return deliverableItemsService.listDeliverableItems(customerId);
    }
}
