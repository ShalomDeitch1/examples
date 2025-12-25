package com.example.ticketmaster.locking.postgresannotations;

import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatReservationService {
    private final SeatInventoryRepository seatInventoryRepository;

    public SeatReservationService(SeatInventoryRepository seatInventoryRepository) {
        this.seatInventoryRepository = seatInventoryRepository;
    }

    /**
     * Returns true if this call successfully reserved the seat.
     * Uses a row lock (PESSIMISTIC_WRITE / SELECT ... FOR UPDATE) inside a transaction.
     */
    @Transactional
    public boolean reservePessimistic(String eventId, String seatId, String orderId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        SeatInventoryId id = new SeatInventoryId(eventId, seatId);
        SeatInventory seat = seatInventoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("seat not found"));

        if (!"AVAILABLE".equals(seat.getStatus())) {
            return false;
        }

        seat.setStatus("RESERVED");
        seat.setOrderId(orderId);
        return true;
    }

    /**
     * Returns true if this call successfully reserved the seat.
     * Uses JPA optimistic locking via @Version.
     */
    @Transactional
    public boolean reserveOptimistic(String eventId, String seatId, String orderId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        SeatInventoryId id = new SeatInventoryId(eventId, seatId);
        SeatInventory seat = seatInventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("seat not found"));

        if (!"AVAILABLE".equals(seat.getStatus())) {
            return false;
        }

        seat.setStatus("RESERVED");
        seat.setOrderId(orderId);

        try {
            seatInventoryRepository.flush();
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            return false;
        }
    }
}
