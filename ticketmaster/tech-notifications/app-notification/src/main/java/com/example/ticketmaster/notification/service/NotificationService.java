package com.example.ticketmaster.notification.service;

import com.example.ticketmaster.notification.model.NotificationStatus;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages automatic state transitions for waiting room notifications.
 * 
 * State transition flow:
 * NOT_READY (2s) -> WAITING (3s) -> READY
 * 
 * When a client calls start(), the service begins automatic state transitions
 * with delays that allow clients to observe each state during polling.
 */
@Service
public class NotificationService {

    private final ConcurrentHashMap<String, String> userStatuses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public NotificationStatus getStatus(String userId) {
        String status = userStatuses.get(userId);
        
        if (status == null) {
            return NotificationStatus.notFound(userId);
        }
        
        return switch (status) {
            case "READY" -> NotificationStatus.ready(userId);
            case "WAITING" -> NotificationStatus.waiting(userId);
            case "NOT_READY" -> NotificationStatus.notReady(userId);
            default -> NotificationStatus.notFound(userId);
        };
    }

    /**
     * Starts the automatic state transition process for a user.
     * 
     * Initial state: NOT_READY
     * After 2 seconds: transitions to WAITING
     * After 3 more seconds (5 total): transitions to READY
     * 
     * This is a 
     */
    public void startProcess(String userId) {
        userStatuses.put(userId, "NOT_READY");
        
        // Transition to WAITING after 2 seconds
        scheduler.schedule(() -> {
            if (userStatuses.containsKey(userId)) {
                userStatuses.put(userId, "WAITING");
            }
        }, 2, TimeUnit.SECONDS);
        
        // Transition to READY after 5 seconds total
        scheduler.schedule(() -> {
            if (userStatuses.containsKey(userId)) {
                userStatuses.put(userId, "READY");
            }
        }, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
