package com.example.localdelivery.simple.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.localdelivery.simple.model.Customer;
import com.example.localdelivery.simple.model.DeliverableItem;
import com.example.localdelivery.simple.repositories.CustomerRepository;
import com.example.localdelivery.simple.service.DeliverableItemsService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemsController {

    private final DeliverableItemsService deliverableItemsService;
    private final CustomerRepository customerRepository;

    public ItemsController(DeliverableItemsService deliverableItemsService, CustomerRepository customerRepository) {
        this.deliverableItemsService = deliverableItemsService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<DeliverableItem> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown customerId"));
            return deliverableItemsService.listDeliverableItems(customer.latitude(), customer.longitude());
        }

        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Provide either customerId or lat+lon");
        }

        return deliverableItemsService.listDeliverableItems(lat, lon);
    }
}
