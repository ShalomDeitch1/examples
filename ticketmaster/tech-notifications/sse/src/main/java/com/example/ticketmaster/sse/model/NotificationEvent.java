package com.example.ticketmaster.sse.model;

public record NotificationEvent(
    String userId,
    String status,
    String message,
    long timestamp
) {
    
    public static NotificationEvent notReady(String userId) {
        return new NotificationEvent(
            userId,
            "NOT_READY",
            "Your request is being processed",
            System.currentTimeMillis()
        );
    }
    
    public static NotificationEvent waiting(String userId) {
        return new NotificationEvent(
            userId,
            "WAITING",
            "You are in the waiting room",
            System.currentTimeMillis()
        );
    }

    public static NotificationEvent ready(String userId) {
        return new NotificationEvent(
            userId,
            "READY",
            "You can now proceed to ticket selection",
            System.currentTimeMillis()
        );
    }
}
