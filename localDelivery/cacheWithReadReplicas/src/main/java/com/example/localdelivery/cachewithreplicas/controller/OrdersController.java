package com.example.localdelivery.cachewithreplicas.controller;

import com.example.localdelivery.cachewithreplicas.model.OrderResponse;
import com.example.localdelivery.cachewithreplicas.model.CreateOrderRequest;
import com.example.localdelivery.cachewithreplicas.model.ConfirmPaymentRequest;
import com.example.localdelivery.cachewithreplicas.service.OrderService;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @PostMapping("/{orderId}/confirm-payment")
    public OrderResponse confirmPayment(@PathVariable UUID orderId, @RequestBody ConfirmPaymentRequest req) {
        return orderService.confirmPayment(orderId, req.success());
    }
}
