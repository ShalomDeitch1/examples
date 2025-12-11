package com.example.directS3.controller;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.directS3.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/hooks/s3")
public class S3EventController {

    /*
     * S3EventController
     *
     * Purpose:
     *  - Receive S3 event notifications (delivered as JSON POST bodies). In local/dev this
     *    endpoint is useful for testing S3 -> SNS -> HTTP deliveries or for wiring test hooks.
     *
     * Expectations / requirements:
     *  - Expects a JSON payload containing either an S3 event ("Records": [...]) or an
     *    SNS envelope (e.g. { "Type":"Notification", "Message":"{...}" }).
     *  - Keys in S3 events may be URL-encoded; this controller decodes them before processing.
     *
     * Production notes (what would be different in prod):
     *  - Verify SNS message signatures (AWS Signature V4) before acting on messages.
     *  - Handle SNS subscription confirmation automatically and securely (this example
     *    performs a simple GET on SubscribeURL when present; in prod verify origin and
     *    signature first).
     *  - Consider making processing idempotent, asynchronous, and resilient to retries.
     */

    private static final Logger log = LoggerFactory.getLogger(S3EventController.class);
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    
    // Hook to perform SubscribeURL confirmations. Default performs a simple GET using HttpClient.
    // Tests can replace this via `setSubscribeUrlExecutor`.
    private java.util.function.Consumer<String> subscribeUrlExecutor = subscribeUrl -> {
        try {
            HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(URI.create(subscribeUrl)).GET().build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to perform SubscribeURL GET", e);
        }
    };

    public S3EventController(FileService fileService, ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    // For tests: allow injection of a custom executor to avoid real HTTP calls.
    void setSubscribeUrlExecutor(java.util.function.Consumer<String> executor) {
        this.subscribeUrlExecutor = executor;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> handleEvent(@RequestBody String body) {
        log.info("Received S3 Event: {}", body);

        try {
            JsonNode root = objectMapper.readTree(body);
            // Handle SNS subscription confirmation messages
            if (isSubscriptionConfirmation(root)) {
                handleSubscriptionConfirmation(root);
                return ResponseEntity.ok().build();
            }
            if (root.has("Records")) {
                handleS3Event(root);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse S3 event", e);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    // package-private for unit tests
    boolean isSubscriptionConfirmation(JsonNode root) {
        return root.has("Type") && "SubscriptionConfirmation".equals(root.get("Type").asText());
    }

    // package-private so tests can invoke/verify without performing real HTTP calls
    void handleSubscriptionConfirmation(JsonNode root) {
        log.info("Received SNS SubscriptionConfirmation");
        if (root.has("SubscribeURL")) {
            String subscribeUrl = root.get("SubscribeURL").asText();
            subscribeUrlExecutor.accept(subscribeUrl);
            log.info("Dispatched SubscribeURL to executor");
        }
    }

    // package-private so it can be unit tested
    void handleS3Event(JsonNode root) {
        for (JsonNode record : root.get("Records")) {
            if (record.has("s3") && record.get("s3").has("object")) {
                String key = record.get("s3").get("object").get("key").asText();
                // Keys coming from S3 events might be URL encoded
                String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
                log.info("Processing ObjectCreated for key: {}", decodedKey);
                try {
                    fileService.markAsAvailable(decodedKey);
                } catch (RuntimeException e) {
                    log.error("Failed to mark file as available for key: " + decodedKey, e);
                }
            }
        }
    }
}
