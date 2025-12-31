package com.example.ticketmaster.longpolling.model;

public record NotificationStatus(
    String userId,
    String status,
    String message,
    long timestamp
) {
    
    public static NotificationStatus notReady(String userId) {
        return new NotificationStatus(
            userId,
            "NOT_READY",
            "Your request is being processed",
            System.currentTimeMillis()
        );
    }
    
    public static NotificationStatus waiting(String userId) {
        return new NotificationStatus(
            userId,
            "WAITING",
            "You are in the waiting room",
            System.currentTimeMillis()
        );
    }

    public static NotificationStatus ready(String userId) {
        return new NotificationStatus(
            userId,
            "READY",
            "You can now proceed to ticket selection",
            System.currentTimeMillis()
        );
    }

    public static NotificationStatus notFound(String userId) {
        return new NotificationStatus(
            userId,
            "NOT_FOUND",
            "No notification found for this user",
            System.currentTimeMillis()
        );
    }
}
