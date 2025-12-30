package com.example.ticketmaster.waitingroom.core;

import java.time.Instant;

public record Request(
    String id,
    String eventId,
    String userId,
    RequestStatus status,
    Instant createdAt,
    Instant processedAt
) {
}
