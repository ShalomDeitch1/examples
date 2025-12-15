package com.example.rollingChunks.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DeviceCheckpoint {

    @Id
    private String deviceId;

    /** The last change_event.id this device has acknowledged (inclusive). */
    private long lastSeenEventId;

    private Instant updatedAt;

    protected DeviceCheckpoint() {}

    public static DeviceCheckpoint newDevice(String deviceId) {
        DeviceCheckpoint c = new DeviceCheckpoint();
        c.deviceId = deviceId;
        c.lastSeenEventId = 0L;
        c.updatedAt = Instant.now();
        return c;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getLastSeenEventId() {
        return lastSeenEventId;
    }

    public void advanceTo(long lastSeenEventId) {
        if (lastSeenEventId > this.lastSeenEventId) {
            this.lastSeenEventId = lastSeenEventId;
            this.updatedAt = Instant.now();
        }
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
