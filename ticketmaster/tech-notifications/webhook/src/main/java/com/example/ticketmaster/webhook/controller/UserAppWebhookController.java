package com.example.ticketmaster.webhook.controller;

import com.example.ticketmaster.webhook.model.WaitingRoomActiveWebhook;
import com.example.ticketmaster.webhook.service.UserAppWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-app")
/**
 * User-app side endpoints for this demo.
 * <p>
 * Why: in real life, the user app is a separate service that exposes a callback URL for Ticketmaster.
 * Here we host both sides in one Spring Boot app to make it runnable with a single `mvn spring-boot:run`.
 */
public class UserAppWebhookController {

    private final UserAppWebhookService userAppWebhookService;

    public UserAppWebhookController(UserAppWebhookService userAppWebhookService) {
        this.userAppWebhookService = userAppWebhookService;
    }

    @PostMapping("/webhooks/waiting-room")
    public ResponseEntity<String> receive(
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Signature-Timestamp") String timestamp,
            @RequestBody WaitingRoomActiveWebhook payload
    ) {
        userAppWebhookService.receive(signature, timestamp, payload);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/inbox/{userId}")
    public ResponseEntity<List<WaitingRoomActiveWebhook>> inbox(@PathVariable String userId) {
        return ResponseEntity.ok(userAppWebhookService.inbox(userId));
    }
}
