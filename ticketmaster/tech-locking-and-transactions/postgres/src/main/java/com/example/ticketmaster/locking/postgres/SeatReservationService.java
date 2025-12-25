package com.example.ticketmaster.locking.postgres;

import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SeatReservationService {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SeatReservationService(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Returns true if this call successfully reserved the seat.
     * Uses a row lock (SELECT ... FOR UPDATE) inside a transaction.
     */
    public boolean reservePessimistic(String eventId, String seatId, String orderId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "select status from seat_inventory where event_id = ? and seat_id = ? for update",
                    eventId,
                    seatId
            );

            String currentStatus = (String) row.get("status");
            if (!"AVAILABLE".equals(currentStatus)) {
                return false;
            }

            int updated = jdbcTemplate.update(
                    "update seat_inventory set status = 'RESERVED', order_id = ? where event_id = ? and seat_id = ?",
                    orderId,
                    eventId,
                    seatId
            );

            return updated == 1;
        }));
    }

    /**
     * Returns true if this call successfully reserved the seat.
     * Uses a version column: UPDATE ... WHERE version = expectedVersion.
     */
    public boolean reserveOptimistic(String eventId, String seatId, String orderId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select status, version from seat_inventory where event_id = ? and seat_id = ?",
                eventId,
                seatId
        );

        String currentStatus = (String) row.get("status");
        if (!"AVAILABLE".equals(currentStatus)) {
            return false;
        }

        long expectedVersion = ((Number) row.get("version")).longValue();
        int updated = jdbcTemplate.update(
                "update seat_inventory set status = 'RESERVED', order_id = ?, version = version + 1 " +
                        "where event_id = ? and seat_id = ? and status = 'AVAILABLE' and version = ?",
                orderId,
                eventId,
                seatId,
                expectedVersion
        );

        return updated == 1;
    }
}
