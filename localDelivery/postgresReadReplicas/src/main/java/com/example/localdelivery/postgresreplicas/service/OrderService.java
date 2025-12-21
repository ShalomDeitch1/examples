package com.example.localdelivery.postgresreplicas.service;

import com.example.localdelivery.postgresreplicas.dao.ReadDao;
import com.example.localdelivery.postgresreplicas.dao.WriteDao;
import com.example.localdelivery.postgresreplicas.model.Models;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private static final int MAX_TRAVEL_TIME_SECONDS = 3600;

    private final ReadDao readDao;
    private final WriteDao writeDao;
    private final TravelTimeService travelTimeService;

    public OrderService(ReadDao readDao, WriteDao writeDao, TravelTimeService travelTimeService) {
        this.readDao = readDao;
        this.writeDao = writeDao;
        this.travelTimeService = travelTimeService;
    }

    /**
     * Write path: uses the primary datasource via WriteDao.
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Models.Customer customer = readDao.findCustomer(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown customerId"));

        UUID orderId = UUID.randomUUID();
        Models.Order order = new Models.Order(orderId, customer.id(), Models.OrderStatus.PENDING_PAYMENT, Instant.now());
        writeDao.insertOrder(order);

        List<Models.OrderLine> lines = new ArrayList<>();
        for (OrderLineRequest line : request.lines()) {
            UUID warehouseId = reserveOneLine(customer, line.itemId(), line.qty());
            Models.OrderLine saved = new Models.OrderLine(orderId, line.itemId(), warehouseId, line.qty());
            writeDao.insertOrderLine(saved);
            lines.add(saved);
        }

        return new OrderResponse(orderId, customer.id(), order.status(), order.createdAt(), lines);
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public OrderResponse confirmPayment(UUID orderId, boolean success) {
        Models.Order order = writeDao.findOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown orderId"));

        if (order.status() != Models.OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order is not pending payment");
        }

        List<Models.OrderLine> lines = writeDao.findLines(orderId);
        if (success) {
            for (Models.OrderLine line : lines) {
                writeDao.consumeReservation(line.warehouseId(), line.itemId(), line.qty());
            }
            writeDao.updateStatus(orderId, Models.OrderStatus.PAID);
            return new OrderResponse(orderId, order.customerId(), Models.OrderStatus.PAID, order.createdAt(), lines);
        }

        for (Models.OrderLine line : lines) {
            writeDao.releaseReservation(line.warehouseId(), line.itemId(), line.qty());
        }
        writeDao.updateStatus(orderId, Models.OrderStatus.PAYMENT_FAILED);
        return new OrderResponse(orderId, order.customerId(), Models.OrderStatus.PAYMENT_FAILED, order.createdAt(), lines);
    }

    private UUID reserveOneLine(Models.Customer customer, UUID itemId, int qty) {
        List<Candidate> candidates = eligibleWarehousesSorted(customer);
        for (Candidate c : candidates) {
            if (writeDao.reserve(c.warehouseId(), itemId, qty)) {
                return c.warehouseId();
            }
        }
        throw new IllegalStateException("Insufficient inventory for item " + itemId);
    }

    private List<Candidate> eligibleWarehousesSorted(Models.Customer customer) {
        List<Candidate> out = new ArrayList<>();
        for (Models.Warehouse w : readDao.findWarehouses()) {
            int seconds = travelTimeService.estimateSeconds(w.latitude(), w.longitude(), customer.latitude(), customer.longitude());
            if (seconds <= MAX_TRAVEL_TIME_SECONDS) {
                out.add(new Candidate(w.id(), seconds));
            }
        }
        out.sort(Comparator.comparingInt(Candidate::travelSeconds));
        return out;
    }

    private record Candidate(UUID warehouseId, int travelSeconds) {}

    public record CreateOrderRequest(@NotNull UUID customerId, @NotEmpty @Valid List<OrderLineRequest> lines) {}

    public record OrderLineRequest(@NotNull UUID itemId, @Min(1) int qty) {}

    public record ConfirmPaymentRequest(@NotNull Boolean success) {}

    public record OrderResponse(UUID orderId, UUID customerId, Models.OrderStatus status, Instant createdAt, List<Models.OrderLine> lines) {}
}
