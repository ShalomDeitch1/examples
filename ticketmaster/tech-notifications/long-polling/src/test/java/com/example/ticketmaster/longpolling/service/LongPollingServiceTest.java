package com.example.ticketmaster.longpolling.service;

import com.example.ticketmaster.longpolling.model.NotificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class LongPollingServiceTest {

    private final LongPollingService service = new LongPollingService();

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void testGetStatusReturnsNotFoundForNonExistentUser() {
        NotificationStatus status = service.getStatus("unknown", null);
        
        assertEquals("NOT_FOUND", status.status());
        assertEquals("unknown", status.userId());
    }

    @Test
    void testStartProcessInitializesWithNotReady() {
        String userId = "user1";
        service.startProcess(userId);
        
        NotificationStatus status = service.getStatus(userId, null);
        
        assertEquals("NOT_READY", status.status());
        assertEquals(userId, status.userId());
    }

    @Test
    void testLongPollingWaitsForStateChange() {
        String userId = "user2";
        service.startProcess(userId);
        
        // First call returns immediately with NOT_READY
        NotificationStatus firstStatus = service.getStatus(userId, null);
        assertEquals("NOT_READY", firstStatus.status());
        
        // Start long-polling in background (should wait for state change)
        long startTime = System.currentTimeMillis();
        CompletableFuture<NotificationStatus> futureStatus = CompletableFuture.supplyAsync(
            () -> service.getStatus(userId, "NOT_READY")
        );
        
        // Wait for the result (should return when state changes to WAITING after 2s)
        NotificationStatus secondStatus = futureStatus.join();
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("WAITING", secondStatus.status());
        assertTrue(duration >= 2000, "Should have waited for state change (~2s)");
        assertTrue(duration < 10000, "Should not have waited for full timeout");
    }

    @Test
    void testStateTransitionsFromNotReadyToWaitingToReady() {
        String userId = "user3";
        service.startProcess(userId);
        
        // Initial state: NOT_READY
        NotificationStatus status1 = service.getStatus(userId, null);
        assertEquals("NOT_READY", status1.status());
        
        // After 2 seconds: WAITING
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            NotificationStatus status = service.getStatus(userId, "NOT_READY");
            assertEquals("WAITING", status.status());
        });
        
        // After 5 seconds total: READY
        await().atMost(4, TimeUnit.SECONDS).untilAsserted(() -> {
            NotificationStatus status = service.getStatus(userId, "WAITING");
            assertEquals("READY", status.status());
        });
    }

    @Test
    void testLongPollingReturnsImmediatelyIfStatusAlreadyChanged() {
        String userId = "user4";
        service.startProcess(userId);
        
        // Wait for state to change to WAITING
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            NotificationStatus status = service.getStatus(userId, "NOT_READY");
            assertEquals("WAITING", status.status());
        });
        
        // Now request with old lastKnownStatus - should return immediately
        long startTime = System.currentTimeMillis();
        NotificationStatus status = service.getStatus(userId, "NOT_READY");
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("WAITING", status.status());
        assertTrue(duration < 500, "Should return immediately when status already changed");
    }
}
