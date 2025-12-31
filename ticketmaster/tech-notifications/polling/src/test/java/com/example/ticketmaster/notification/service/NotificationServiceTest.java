package com.example.ticketmaster.notification.service;

import com.example.ticketmaster.notification.model.NotificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

/**
 * Tests for NotificationService focusing on automatic state transitions.
 * 
 * The service transitions states as follows:
 * NOT_READY (2s) -> WAITING (3s) -> READY
 */
class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void getStatus_shouldReturnNotFound_whenUserDoesNotExist() {
        NotificationStatus status = service.getStatus("unknown");

        assertThat(status.status()).isEqualTo("NOT_FOUND");
        assertThat(status.userId()).isEqualTo("unknown");
    }

    @Test
    void startProcess_shouldInitiallySetStatusToNotReady() {
        String userId = "user123";
        service.startProcess(userId);

        NotificationStatus status = service.getStatus(userId);

        assertThat(status.status()).isEqualTo("NOT_READY");
        assertThat(status.userId()).isEqualTo(userId);
        assertThat(status.message()).isEqualTo("Your request is being processed");
    }

    @Test
    void startProcess_shouldTransitionToWaiting_after2Seconds() {
        String userId = "user456";
        service.startProcess(userId);

        // Verify initial state
        assertThat(service.getStatus(userId).status()).isEqualTo("NOT_READY");

        // Wait and verify transition to WAITING (occurs at 2 seconds)
        await().atMost(3, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   NotificationStatus status = service.getStatus(userId);
                   assertThat(status.status()).isEqualTo("WAITING");
                   assertThat(status.message()).isEqualTo("You are in the waiting room");
               });
    }

    @Test
    void startProcess_shouldTransitionToReady_after5Seconds() {
        String userId = "user789";
        service.startProcess(userId);

        // Verify initial state
        assertThat(service.getStatus(userId).status()).isEqualTo("NOT_READY");

        // Wait and verify transition to READY (occurs at 5 seconds)
        await().atMost(6, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   NotificationStatus status = service.getStatus(userId);
                   assertThat(status.status()).isEqualTo("READY");
                   assertThat(status.message()).isEqualTo("You can now proceed to ticket selection");
               });
    }

    @Test
    void startProcess_shouldObserveAllThreeStates_whenPollingRegularly() throws InterruptedException {
        String userId = "user321";
        service.startProcess(userId);

        // Observe NOT_READY
        NotificationStatus notReadyStatus = service.getStatus(userId);
        assertThat(notReadyStatus.status()).isEqualTo("NOT_READY");

        // Wait 2.5 seconds and observe WAITING
        Thread.sleep(2500);
        NotificationStatus waitingStatus = service.getStatus(userId);
        assertThat(waitingStatus.status()).isEqualTo("WAITING");

        // Wait another 3 seconds and observe READY
        Thread.sleep(3000);
        NotificationStatus readyStatus = service.getStatus(userId);
        assertThat(readyStatus.status()).isEqualTo("READY");
    }

    @Test
    void multipleUsers_shouldHaveIndependentStateTransitions() {
        String user1 = "alice";
        String user2 = "bob";

        service.startProcess(user1);
        
        // Start user2 1 second later
        await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
        service.startProcess(user2);

        // At ~1.5 seconds: user1 should be NOT_READY, user2 should be NOT_READY
        assertThat(service.getStatus(user1).status()).isEqualTo("NOT_READY");
        assertThat(service.getStatus(user2).status()).isEqualTo("NOT_READY");

        // At ~2.5 seconds: user1 should be WAITING, user2 should be NOT_READY
        await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
        assertThat(service.getStatus(user1).status()).isEqualTo("WAITING");
        assertThat(service.getStatus(user2).status()).isEqualTo("NOT_READY");
    }
}
