package com.example.ticketmaster.waitingroom.core;

import java.time.Instant;

public record WaitingRoomRequest(
    String id,
    String eventId,
    String userId,
    WaitingRoomRequestStatus status,
    Instant createdAt,
    Instant processedAt
) {
}
