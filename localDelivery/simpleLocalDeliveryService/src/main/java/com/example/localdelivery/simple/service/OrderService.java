package com.example.localdelivery.simple.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.localdelivery.simple.model.CreateOrderRequest;
import com.example.localdelivery.simple.model.Customer;
import com.example.localdelivery.simple.model.Order;
import com.example.localdelivery.simple.model.OrderResponse;
import com.example.localdelivery.simple.model.OrderStatus;
import com.example.localdelivery.simple.model.Warehouse;
import com.example.localdelivery.simple.repositories.CustomerRepository;
import com.example.localdelivery.simple.repositories.InventoryRepository;
import com.example.localdelivery.simple.repositories.OrderRepository;
import com.example.localdelivery.simple.repositories.WarehouseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.example.localdelivery.simple.model.OrderLine;
import com.example.localdelivery.simple.model.OrderLineRequest;

@Service
public class OrderService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600; // 1 hour

    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final TravelTimeService travelTimeService;

    public OrderService(
            CustomerRepository customerRepository,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            TravelTimeService travelTimeService
    ) {
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.travelTimeService = travelTimeService;
    }

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown customerId"));

        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, customer.id(), OrderStatus.PENDING_PAYMENT, Instant.now());
        orderRepository.insertOrder(order);

        List<OrderLine> createdLines = new ArrayList<>();
        for (OrderLineRequest line : request.lines()) {
            Reservation reservation = reserveOneLine(customer, line.itemId(), line.qty());
            OrderLine orderLine = new OrderLine(orderId, line.itemId(), reservation.warehouseId(), line.qty());
            orderRepository.insertOrderLine(orderLine);
            createdLines.add(orderLine);
        }

        return new OrderResponse(orderId, customer.id(), order.status(), order.createdAt(), createdLines);
    }

    @Transactional
    public OrderResponse confirmPayment(UUID orderId, boolean success) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown orderId"));

        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order is not pending payment");
        }

        List<OrderLine> lines = orderRepository.findLines(orderId);

        if (success) {
            for (OrderLine line : lines) {
                inventoryRepository.consumeReservation(line.warehouseId(), line.itemId(), line.qty());
            }
            orderRepository.updateStatus(orderId, OrderStatus.PAID);
            return new OrderResponse(orderId, order.customerId(), OrderStatus.PAID, order.createdAt(), lines);
        } else {
            for (OrderLine line : lines) {
                inventoryRepository.releaseReservation(line.warehouseId(), line.itemId(), line.qty());
            }
            orderRepository.updateStatus(orderId, OrderStatus.PAYMENT_FAILED);
            return new OrderResponse(orderId, order.customerId(), OrderStatus.PAYMENT_FAILED, order.createdAt(), lines);
        }
    }

    private Reservation reserveOneLine(Customer customer, UUID itemId, int qty) {
        List<WarehouseCandidate> candidates = findEligibleWarehousesSorted(customer);

        for (WarehouseCandidate candidate : candidates) {
            boolean ok = inventoryRepository.reserve(candidate.warehouse.id(), itemId, qty);
            if (ok) {
                return new Reservation(candidate.warehouse.id());
            }
        }

        throw new IllegalStateException("Insufficient inventory for item " + itemId);
    }

    private List<WarehouseCandidate> findEligibleWarehousesSorted(Customer customer) {
        List<WarehouseCandidate> candidates = new ArrayList<>();
        for (Warehouse w : warehouseRepository.findAll()) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), customer.latitude(), customer.longitude());
            if (seconds <= MAX_TRAVEL_TIME_SECONDS) {
                candidates.add(new WarehouseCandidate(w, seconds));
            }
        }

        candidates.sort(Comparator.comparingInt(WarehouseCandidate::travelSeconds));
        return candidates;
    }

    private record WarehouseCandidate(Warehouse warehouse, int travelSeconds) {}
    private record Reservation(UUID warehouseId) {}
}
