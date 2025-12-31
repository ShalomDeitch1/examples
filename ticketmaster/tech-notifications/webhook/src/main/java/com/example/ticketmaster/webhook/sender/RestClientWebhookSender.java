package com.example.ticketmaster.webhook.sender;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RestClientWebhookSender implements WebhookSender {

    /**
     * Minimal HTTP client for webhook delivery.
     * <p>
     * Why RestClient: it's the simplest Spring abstraction for making outbound HTTP requests in this demo.
     * The receiver verifies the headers set here ({@code X-Signature} and {@code X-Signature-Timestamp}).
     */

    private final RestClient restClient;

    public RestClientWebhookSender(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void send(String callbackUrl, WaitingRoomActiveWebhook payload, long timestampSeconds, String signature) {
        restClient
                .post()
                .uri(callbackUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .header("X-Signature-Timestamp", Long.toString(timestampSeconds))
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
