package com.example.ticketmaster.webhook.service;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import com.example.ticketmaster.webhook.sender.WebhookSender;
import com.example.ticketmaster.webhook.signature.WebhookSigner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class WebhookTicketmasterServiceTest {

    /**
     * Verifies the sender-side contract: the webhook is sent only when the user becomes READY.
     * This matches the "notify when you can proceed" pattern (ACTIVE/READY), not intermediate WAITING updates.
     */

    @Test
    void sendsWebhookWhenUserBecomesReady() {
        List<WaitingRoomActiveWebhook> sent = new ArrayList<>();

        WebhookSender sender = (callbackUrl, payload, ts, sig) -> {
            assertEquals("http://localhost:8080/user-app/webhooks/waiting-room", callbackUrl);
            assertNotNull(sig);
            sent.add(payload);
        };

        WebhookSigner signer = new WebhookSigner("secret");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        WebhookTicketmasterService service = new WebhookTicketmasterService(
                sender,
                signer,
                scheduler,
                Duration.ofMillis(150),
                Duration.ofMillis(350)
        );

        service.registerWebhook("alice", "http://localhost:8080/user-app/webhooks/waiting-room");

        String sessionId = service.startProcess("alice");
        assertTrue(sessionId.startsWith("wr_"));

        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals("READY", service.getStatus("alice").orElseThrow()));
        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals(1, sent.size()));

        service.shutdown();
        scheduler.shutdown();
    }
}
