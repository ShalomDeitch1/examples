package com.example.ticketmaster.webhook.controller;

import com.example.ticketmaster.webhook.model.RegisterWebhookRequest;
import com.example.ticketmaster.webhook.service.WebhookTicketmasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ticketmaster")
/**
 * Ticketmaster-side HTTP API for this demo.
 * <p>
 * Why this controller exists: it gives a simple way (via curl) to (1) register a user callback URL,
 * (2) start a simulated waiting-room flow, and (3) query current status.
 * The actual webhook callback is sent by {@link com.example.ticketmaster.webhook.service.WebhookTicketmasterService}
 * once the user becomes READY.
 */
public class TicketmasterWebhookController {

    private final WebhookTicketmasterService ticketmasterService;

    public TicketmasterWebhookController(WebhookTicketmasterService ticketmasterService) {
        this.ticketmasterService = ticketmasterService;
    }

    @PostMapping("/register/{userId}")
    public ResponseEntity<String> register(@PathVariable String userId, @RequestBody RegisterWebhookRequest request) {
        ticketmasterService.registerWebhook(userId, request.callbackUrl());
        return ResponseEntity.ok("Webhook registered for user %s".formatted(userId));
    }

    @PostMapping("/start/{userId}")
    public ResponseEntity<Map<String, String>> start(@PathVariable String userId) {
        String sessionId = ticketmasterService.startProcess(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "sessionId", sessionId));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String userId) {
        String status = ticketmasterService.getStatus(userId).orElse("NOT_FOUND");
        return ResponseEntity.ok(Map.of("userId", userId, "status", status));
    }
}
