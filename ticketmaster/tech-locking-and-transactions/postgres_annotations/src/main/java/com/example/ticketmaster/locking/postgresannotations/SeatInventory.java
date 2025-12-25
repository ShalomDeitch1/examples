package com.example.ticketmaster.locking.postgresannotations;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "seat_inventory")
public class SeatInventory {
    @EmbeddedId
    private SeatInventoryId id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "order_id")
    private String orderId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SeatInventory() {
    }

    public SeatInventory(SeatInventoryId id, String status, String orderId) {
        this.id = id;
        this.status = status;
        this.orderId = orderId;
    }

    public SeatInventoryId getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getVersion() {
        return version;
    }
}
