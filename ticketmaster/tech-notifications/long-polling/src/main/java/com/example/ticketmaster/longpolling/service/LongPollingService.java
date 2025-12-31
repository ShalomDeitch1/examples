package com.example.ticketmaster.longpolling.service;

import com.example.ticketmaster.longpolling.model.NotificationStatus;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages automatic state transitions for long-polling notifications.
 * 
 * State transition flow:
 * NOT_READY (2s) -> WAITING (3s) -> READY
 * 
 * The long-polling mechanism waits for state changes before responding,
 * reducing the number of requests compared to regular polling.
 */
@Service
public class LongPollingService {

    private final ConcurrentHashMap<String, String> userStatuses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Gets the current status for a user, waiting up to 30 seconds for a state change.
     * 
     * @param userId The user ID to check
     * @param lastKnownStatus The last status the client received (null on first call)
     * @return The current status (immediately if changed, or after timeout)
     */
    public NotificationStatus getStatus(String userId, String lastKnownStatus) {
        String currentStatus = userStatuses.get(userId);
        
        // If no session exists, return NOT_FOUND immediately
        if (currentStatus == null) {
            return NotificationStatus.notFound(userId);
        }
        
        // If status has changed, return immediately
        if (!currentStatus.equals(lastKnownStatus)) {
            return buildStatusResponse(userId, currentStatus);
        }
        
        // Wait for status change (with timeout)
        return waitForStatusChange(userId, lastKnownStatus);
    }

    /**
     * Waits for the status to change, with a 30-second timeout.
     * Returns immediately if status changes, or after timeout with current status.
     */
    private NotificationStatus waitForStatusChange(String userId, String lastKnownStatus) {
        long startTime = System.currentTimeMillis();
        long timeout = 30_000; // 30 seconds
        
        while (System.currentTimeMillis() - startTime < timeout) {
            String currentStatus = userStatuses.get(userId);
            
            // Status changed - return immediately
            if (currentStatus != null && !currentStatus.equals(lastKnownStatus)) {
                return buildStatusResponse(userId, currentStatus);
            }
            
            // User no longer exists
            if (currentStatus == null) {
                return NotificationStatus.notFound(userId);
            }
            
            // Sleep briefly before checking again
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return buildStatusResponse(userId, currentStatus);
            }
        }
        
        // Timeout - return current status
        String currentStatus = userStatuses.get(userId);
        return currentStatus != null 
            ? buildStatusResponse(userId, currentStatus)
            : NotificationStatus.notFound(userId);
    }

    private NotificationStatus buildStatusResponse(String userId, String status) {
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
