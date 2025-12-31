package com.example.ticketmaster.webhook.model;

import java.time.Instant;

public record WaitingRoomActiveWebhook(
        String type,
        String sessionId,
        String userId,
        String eventId,
        Instant issuedAt,
        String idempotencyKey
) {

    /**
     * Demo webhook payload for "user is active / out of the waiting room".
     * <p>
     * Why include an idempotency key: webhook deliveries can be retried and arrive multiple times.
     * The receiver can safely ignore duplicates by remembering this key.
     */
    public static WaitingRoomActiveWebhook waitingRoomActive(String sessionId, String userId) {
        String eventId = "evt_" + Instant.now().toEpochMilli();
        String idempotencyKey = "evt_%s_active".formatted(sessionId);
        return new WaitingRoomActiveWebhook(
                "WAITING_ROOM_ACTIVE",
                sessionId,
                userId,
                eventId,
                Instant.now(),
                idempotencyKey
        );
    }
}
