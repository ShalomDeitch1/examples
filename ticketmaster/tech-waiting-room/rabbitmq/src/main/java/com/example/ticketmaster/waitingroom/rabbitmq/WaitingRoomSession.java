package com.example.ticketmaster.waitingroom.rabbitmq;

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
