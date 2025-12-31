package com.example.ticketmaster.longpolling.controller;

import com.example.ticketmaster.longpolling.model.NotificationStatus;
import com.example.ticketmaster.longpolling.service.LongPollingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LongPollingController {

    private final LongPollingService longPollingService;

    public LongPollingController(LongPollingService longPollingService) {
        this.longPollingService = longPollingService;
    }

    /**
     * Client initiates the waiting room process.
     * This starts automatic state transitions: NOT_READY -> WAITING -> READY
     */
    @PostMapping("/start/{userId}")
    public ResponseEntity<String> startProcess(@PathVariable String userId) {
        longPollingService.startProcess(userId);
        return ResponseEntity.ok("Process started for user %s".formatted(userId));
    }

    /**
     * Long-polling endpoint: client requests status and server holds connection
     * until status changes (or 30-second timeout).
     * 
     * @param userId The user ID to check
     * @param lastStatus Optional query parameter with the last known status
     * @return The current status (immediately if changed, or after timeout)
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<NotificationStatus> getStatus(
            @PathVariable String userId,
            @RequestParam(required = false) String lastStatus) {
        
        NotificationStatus status = longPollingService.getStatus(userId, lastStatus);
        return ResponseEntity.ok(status);
    }
}
