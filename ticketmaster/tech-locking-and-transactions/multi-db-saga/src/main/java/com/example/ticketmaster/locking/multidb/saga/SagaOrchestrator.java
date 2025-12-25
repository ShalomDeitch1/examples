package com.example.ticketmaster.locking.multidb.saga;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SagaOrchestrator {

    private final InventoryStore inventoryStore;
    private final PaymentStore paymentStore;

    public SagaOrchestrator(InventoryStore inventoryStore, PaymentStore paymentStore) {
        this.inventoryStore = inventoryStore;
        this.paymentStore = paymentStore;
    }

    public SagaResult purchase(UUID sagaId, String seatId, int amountCents) {
        Objects.requireNonNull(sagaId);
        Objects.requireNonNull(seatId);

        boolean reserved = inventoryStore.reserve(seatId, sagaId);
        if (!reserved) {
            return new SagaResult(false, "SEAT_NOT_AVAILABLE", inventoryStore.getSeatStatus(seatId), null);
        }

        String paymentStatus = paymentStore.authorize(sagaId, amountCents);
        if (!"AUTHORIZED".equals(paymentStatus)) {
            inventoryStore.releaseIfReservedBySaga(seatId, sagaId); // compensation
            return new SagaResult(false, "PAYMENT_DECLINED", inventoryStore.getSeatStatus(seatId), paymentStatus);
        }

        inventoryStore.confirm(seatId, sagaId);
        return new SagaResult(true, "OK", inventoryStore.getSeatStatus(seatId), paymentStatus);
    }

    public record SagaResult(boolean success, String code, String seatStatus, String paymentStatus) {
    }
}
