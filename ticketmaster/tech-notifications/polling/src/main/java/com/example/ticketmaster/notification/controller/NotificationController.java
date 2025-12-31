package com.example.ticketmaster.notification.controller;

import com.example.ticketmaster.notification.model.NotificationStatus;
import com.example.ticketmaster.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Client initiates the waiting room process.
     * This starts automatic state transitions: NOT_READY -> WAITING -> READY
     */
    @PostMapping("/start/{userId}")
    public ResponseEntity<String> startProcess(@PathVariable String userId) {
        notificationService.startProcess(userId);
        return ResponseEntity.ok("Process started for user %s".formatted(userId));
    }

    /**
     * Client polls this endpoint to check current status.
     * Expected flow: NOT_READY -> WAITING -> READY
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<NotificationStatus> getStatus(@PathVariable String userId) {
        NotificationStatus status = notificationService.getStatus(userId);
        return ResponseEntity.ok(status);
    }
}
