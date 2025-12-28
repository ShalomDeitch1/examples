package com.example.ticketmaster.waitingroom.kafka;

import java.time.Instant;

public record WaitingRoomSession(
    String id,
    String eventId,
    String userId,
    WaitingRoomSessionStatus status,
    Instant createdAt,
    Instant activatedAt,
    Instant lastHeartbeatAt
) {
}
