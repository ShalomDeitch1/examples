package com.example.ticketmaster.webhook.service;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import com.example.ticketmaster.webhook.signature.WebhookSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
/**
 * Webhook receiver-side logic.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Validate the timestamp header (basic replay protection).</li>
 *     <li>Verify the HMAC signature (authenticity + tamper detection).</li>
 *     <li>Deduplicate deliveries using {@code idempotencyKey} (safe retries).</li>
 *     <li>Store accepted events in an in-memory inbox (to keep the demo simple).</li>
 * </ul>
 * <p>
 * Why the {@link java.time.Clock} dependency: it makes timestamp validation deterministic in unit tests.
 */
public class UserAppWebhookService {

    private final WebhookSigner webhookSigner;
    private final Clock clock;
    private final Duration maxClockSkew;

    private final ConcurrentHashMap<String, List<WaitingRoomActiveWebhook>> inbox = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> seenIdempotencyKeys = new ConcurrentHashMap<>();

    public UserAppWebhookService(
            WebhookSigner webhookSigner,
            Clock clock,
            @Value("${webhook.maxClockSkewSeconds:300}") long maxClockSkewSeconds
    ) {
        this.webhookSigner = webhookSigner;
        this.clock = clock;
        this.maxClockSkew = Duration.ofSeconds(maxClockSkewSeconds);
    }

    public void receive(String signatureHeader, String timestampHeader, WaitingRoomActiveWebhook payload) {
        long timestampSeconds = parseTimestamp(timestampHeader);
        validateTimestamp(timestampSeconds);

        if (!webhookSigner.matches(signatureHeader, timestampSeconds, payload.idempotencyKey())) {
            throw new IllegalArgumentException("Invalid signature");
        }

        if (alreadySeen(payload.userId(), payload.idempotencyKey())) {
            return;
        }

        inbox.computeIfAbsent(payload.userId(), ignored -> new CopyOnWriteArrayList<>()).add(payload);
    }

    public List<WaitingRoomActiveWebhook> inbox(String userId) {
        return inbox.getOrDefault(userId, List.of());
    }

    private boolean alreadySeen(String userId, String idempotencyKey) {
        Set<String> seen = seenIdempotencyKeys.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        return !seen.add(idempotencyKey);
    }

    private void validateTimestamp(long timestampSeconds) {
        Instant now = Instant.now(clock);
        Instant ts = Instant.ofEpochSecond(timestampSeconds);
        Duration delta = Duration.between(ts, now).abs();
        if (delta.compareTo(maxClockSkew) > 0) {
            throw new IllegalArgumentException("Timestamp outside allowed skew");
        }
    }

    private static long parseTimestamp(String timestampHeader) {
        try {
            return Long.parseLong(timestampHeader);
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing/invalid X-Signature-Timestamp");
        }
    }
}
