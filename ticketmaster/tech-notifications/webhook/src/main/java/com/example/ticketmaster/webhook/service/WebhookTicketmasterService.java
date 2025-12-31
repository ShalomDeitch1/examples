package com.example.ticketmaster.webhook.service;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import com.example.ticketmaster.webhook.sender.WebhookSender;
import com.example.ticketmaster.webhook.signature.WebhookSigner;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
/**
 * Webhook-based notification flow (Ticketmaster-style):
 * <ol>
 *     <li>Client registers a callback URL: {@link #registerWebhook(String, String)}.</li>
 *     <li>Client starts the waiting-room process: {@link #startProcess(String)}.</li>
 *     <li>Service transitions status over time: {@code NOT_READY -> WAITING -> READY}.</li>
 *     <li>When the user becomes {@code READY} (a.k.a. “ACTIVE / out of the waiting room”), the service sends a
 *     {@link com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook} to the registered callback URL.</li>
 * </ol>
 * <p>
 * Why no webhook for {@code WAITING}? This example models the common real-world contract where the callback is only
 * fired when the user can proceed (ACTIVE/READY). If you want callbacks for intermediate states too, model a different
 * event type (e.g., "StatusChanged") and make the receiver handle multiple event kinds.
 */
public class WebhookTicketmasterService {

    private static final Duration DEFAULT_WAITING_AFTER = Duration.ofSeconds(2);
    private static final Duration DEFAULT_READY_AFTER = Duration.ofSeconds(5);

    private final WebhookSender webhookSender;
    private final WebhookSigner webhookSigner;
    private final ScheduledExecutorService scheduler;

    private final Duration waitingAfter;
    private final Duration readyAfter;

    private final ConcurrentHashMap<String, String> userStatuses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> callbackUrls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIds = new ConcurrentHashMap<>();

    @Autowired
    public WebhookTicketmasterService(WebhookSender webhookSender, WebhookSigner webhookSigner) {
        this(webhookSender, webhookSigner, Executors.newScheduledThreadPool(2), DEFAULT_WAITING_AFTER, DEFAULT_READY_AFTER);
    }

    WebhookTicketmasterService(
            WebhookSender webhookSender,
            WebhookSigner webhookSigner,
            ScheduledExecutorService scheduler,
            Duration waitingAfter,
            Duration readyAfter
    ) {
        this.webhookSender = webhookSender;
        this.webhookSigner = webhookSigner;
        this.scheduler = scheduler;
        this.waitingAfter = waitingAfter;
        this.readyAfter = readyAfter;
    }

    public void registerWebhook(String userId, String callbackUrl) {
        callbackUrls.put(userId, callbackUrl);
    }

    public Optional<String> getStatus(String userId) {
        return Optional.ofNullable(userStatuses.get(userId));
    }

    public String startProcess(String userId) {
        String sessionId = sessionIds.computeIfAbsent(userId, ignored -> "wr_" + UUID.randomUUID());
        userStatuses.put(userId, "NOT_READY");

        scheduler.schedule(() -> userStatuses.put(userId, "WAITING"), waitingAfter.toMillis(), TimeUnit.MILLISECONDS);

        scheduler.schedule(() -> {
            userStatuses.put(userId, "READY");
            sendActiveWebhookIfRegistered(userId, sessionId);
        }, readyAfter.toMillis(), TimeUnit.MILLISECONDS);

        return sessionId;
    }

    private void sendActiveWebhookIfRegistered(String userId, String sessionId) {
        String callbackUrl = callbackUrls.get(userId);
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return;
        }

        WaitingRoomActiveWebhook payload = WaitingRoomActiveWebhook.waitingRoomActive(sessionId, userId);
        long timestampSeconds = Instant.now().getEpochSecond();
        String signature = webhookSigner.signature(timestampSeconds, payload.idempotencyKey());

        webhookSender.send(callbackUrl, payload, timestampSeconds, signature);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
