package com.example.localdelivery.cachingredisgeo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.localdelivery.cachingredisgeo.model.DeliverableItem;
import com.example.localdelivery.cachingredisgeo.service.DeliverableItemsService;

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
