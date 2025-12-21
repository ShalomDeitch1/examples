package com.example.localdelivery.optimized.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.localdelivery.optimized.model.Models;
import com.example.localdelivery.optimized.service.ItemsService;

import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping
    public List<Models.DeliverableItem> list(@RequestParam double lat, @RequestParam double lon) {
        return itemsService.listDeliverableItems(lat, lon);
    }
}
