package com.example.localdelivery.simple.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.localdelivery.simple.model.ConfirmPaymentRequest;
import com.example.localdelivery.simple.model.CreateOrderRequest;
import com.example.localdelivery.simple.model.OrderResponse;
import com.example.localdelivery.simple.service.OrderService;

import java.util.UUID;

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
    public OrderResponse confirmPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        return orderService.confirmPayment(orderId, request.success());
    }
}
