package com.example.localdelivery.optimized;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
