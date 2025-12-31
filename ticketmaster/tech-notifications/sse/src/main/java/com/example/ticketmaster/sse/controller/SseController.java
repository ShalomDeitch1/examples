package com.example.ticketmaster.sse.controller;

import com.example.ticketmaster.sse.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    /**
     * Client initiates the waiting room process.
     * This starts automatic state transitions: NOT_READY -> WAITING -> READY
     */
    @PostMapping("/start/{userId}")
    public ResponseEntity<String> startProcess(@PathVariable String userId) {
        sseService.startProcess(userId);
        return ResponseEntity.ok("Process started for user %s".formatted(userId));
    }

    /**
     * SSE subscription endpoint: client opens a persistent connection
     * and receives real-time status updates as they occur.
     * 
     * The connection stays open and pushes:
     * - NOT_READY (immediately)
     * - WAITING (after ~2 seconds)
     * - READY (after ~5 seconds total)
     * 
     * Connection closes automatically after READY status is sent.
     * 
     * @param userId The user ID to subscribe to updates for
     * @return SseEmitter that will push status events
     */
    @GetMapping(path = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String userId) {
        return sseService.subscribe(userId);
    }
}
