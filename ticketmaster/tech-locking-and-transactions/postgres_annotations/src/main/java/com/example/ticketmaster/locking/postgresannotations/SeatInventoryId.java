package com.example.ticketmaster.locking.postgresannotations;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SeatInventoryId implements Serializable {
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "seat_id", nullable = false)
    private String seatId;

    protected SeatInventoryId() {
    }

    public SeatInventoryId(String eventId, String seatId) {
        this.eventId = Objects.requireNonNull(eventId);
        this.seatId = Objects.requireNonNull(seatId);
    }

    public String getEventId() {
        return eventId;
    }

    public String getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SeatInventoryId that = (SeatInventoryId) o;
        return Objects.equals(eventId, that.eventId) && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, seatId);
    }
}
