package com.example.ticketmaster.locking.multidb.saga;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryStore {

    private final JdbcTemplate jdbc;

    public InventoryStore(@Qualifier("inventoryJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean reserve(String seatId, UUID sagaId) {
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(sagaId);

        int updated = jdbc.update(
                "UPDATE seats SET status='RESERVED', reserved_by=?::uuid WHERE seat_id=? AND status='AVAILABLE'",
                sagaId.toString(),
                seatId
        );
        if (updated == 1) {
            return true;
        }

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, reserved_by FROM seats WHERE seat_id=?",
                seatId
        );

        String status = (String) row.get("status");
        Object reservedBy = row.get("reserved_by");

        if (reservedBy instanceof UUID reservedByUuid) {
            return reservedByUuid.equals(sagaId) && ("RESERVED".equals(status) || "CONFIRMED".equals(status));
        }

        return false;
    }

    public void confirm(String seatId, UUID sagaId) {
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(sagaId);

        int updated = jdbc.update(
                "UPDATE seats SET status='CONFIRMED' WHERE seat_id=? AND reserved_by=?::uuid AND status='RESERVED'",
                seatId,
                sagaId.toString()
        );
        if (updated == 1) {
            return;
        }

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, reserved_by FROM seats WHERE seat_id=?",
                seatId
        );
        String status = (String) row.get("status");
        Object reservedBy = row.get("reserved_by");

        if ("CONFIRMED".equals(status) && reservedBy instanceof UUID reservedByUuid && reservedByUuid.equals(sagaId)) {
            return; // idempotent retry
        }

        throw new IllegalStateException("Cannot confirm seat " + seatId + " for saga " + sagaId);
    }

    public void releaseIfReservedBySaga(String seatId, UUID sagaId) {
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(sagaId);

        jdbc.update(
                "UPDATE seats SET status='AVAILABLE', reserved_by=NULL WHERE seat_id=? AND reserved_by=?::uuid AND status='RESERVED'",
                seatId,
                sagaId.toString()
        );
    }

    public String getSeatStatus(String seatId) {
        return jdbc.queryForObject("SELECT status FROM seats WHERE seat_id=?", String.class, seatId);
    }
}
