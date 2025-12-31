package com.example.ticketmaster.webhook.service;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import com.example.ticketmaster.webhook.signature.WebhookSigner;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class UserAppWebhookServiceTest {

    /**
     * Verifies the receiver-side behaviors that make webhooks safe in production:
     * - signature verification rejects spoofed requests
     * - timestamp validation rejects replayed/stale requests
     * - idempotency prevents duplicated processing when the sender retries
     */

    @Test
    void acceptsValidSignatureAndStoresEventOnce() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        WebhookSigner signer = new WebhookSigner("secret");
        UserAppWebhookService service = new UserAppWebhookService(signer, clock, 300);

        WaitingRoomActiveWebhook payload = WaitingRoomActiveWebhook.waitingRoomActive("wr_1", "alice");
        long ts = 1_700_000_000L;
        String sig = signer.signature(ts, payload.idempotencyKey());

        service.receive(sig, Long.toString(ts), payload);
        service.receive(sig, Long.toString(ts), payload);

        assertEquals(1, service.inbox("alice").size());
    }

    @Test
    void rejectsInvalidSignature() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        WebhookSigner signer = new WebhookSigner("secret");
        UserAppWebhookService service = new UserAppWebhookService(signer, clock, 300);

        WaitingRoomActiveWebhook payload = WaitingRoomActiveWebhook.waitingRoomActive("wr_1", "alice");
        long ts = 1_700_000_000L;

        assertThrows(IllegalArgumentException.class, () -> service.receive("hmac-sha256=bad", Long.toString(ts), payload));
    }

    @Test
    void rejectsOldTimestamp() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_500L), ZoneOffset.UTC);
        WebhookSigner signer = new WebhookSigner("secret");
        UserAppWebhookService service = new UserAppWebhookService(signer, clock, 300);

        WaitingRoomActiveWebhook payload = WaitingRoomActiveWebhook.waitingRoomActive("wr_1", "alice");
        long oldTs = 1_700_000_000L;
        String sig = signer.signature(oldTs, payload.idempotencyKey());

        assertThrows(IllegalArgumentException.class, () -> service.receive(sig, Long.toString(oldTs), payload));
    }
}
