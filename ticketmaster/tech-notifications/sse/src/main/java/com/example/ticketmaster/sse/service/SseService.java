package com.example.ticketmaster.sse.service;

import com.example.ticketmaster.sse.model.NotificationEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages SSE connections and automatic state transitions.
 * 
 * State transition flow:
 * NOT_READY (2s) -> WAITING (3s) -> READY
 * 
 * When a client subscribes, the server keeps the connection open and pushes
 * status updates as they occur, eliminating the need for repeated requests.
 */
@Service
public class SseService {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userStatuses = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final SseEmitterFactory sseEmitterFactory;

    @Autowired
    public SseService(SseEmitterFactory sseEmitterFactory) {
        this(sseEmitterFactory, new ExecutorScheduler(Executors.newScheduledThreadPool(2)));
    }

    SseService(SseEmitterFactory sseEmitterFactory, Scheduler scheduler) {
        this.sseEmitterFactory = sseEmitterFactory;
        this.scheduler = scheduler;
    }

    /**
     * Creates an SSE subscription for a user.
     * The connection stays open and receives all status updates.
     * 
     * @param userId The user ID to subscribe to
     * @return SseEmitter that will push status events
     */
    public SseEmitter subscribe(String userId) {
        // 5-minute timeout (can be configured)
        SseEmitter emitter = sseEmitterFactory.create(300_000L);
        
        emitters.put(userId, emitter);
        
        // Clean up on completion, timeout, or error
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        
        // Send current status immediately if session exists
        String currentStatus = userStatuses.get(userId);
        if (currentStatus != null) {
            sendEvent(emitter, buildEvent(userId, currentStatus));
        }
        
        return emitter;
    }

    /**
     * Starts the automatic state transition process for a user.
     * 
     * Initial state: NOT_READY
     * After 2 seconds: transitions to WAITING
     * After 3 more seconds (5 total): transitions to READY
     * 
     * Each transition is pushed to subscribed clients via SSE.
     */
    public void startProcess(String userId) {
        userStatuses.put(userId, "NOT_READY");
        
        // Send initial status to any existing subscribers
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            sendEvent(emitter, NotificationEvent.notReady(userId));
        }
        
        // Transition to WAITING after 2 seconds
        scheduler.schedule(() -> {
            if (userStatuses.containsKey(userId)) {
                userStatuses.put(userId, "WAITING");
                SseEmitter e = emitters.get(userId);
                if (e != null) {
                    sendEvent(e, NotificationEvent.waiting(userId));
                }
            }
        }, 2, TimeUnit.SECONDS);
        
        // Transition to READY after 5 seconds total
        scheduler.schedule(() -> {
            if (userStatuses.containsKey(userId)) {
                userStatuses.put(userId, "READY");
                SseEmitter e = emitters.get(userId);
                if (e != null) {
                    sendEvent(e, NotificationEvent.ready(userId));
                    // Complete the emitter after sending final status
                    e.complete();
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    private NotificationEvent buildEvent(String userId, String status) {
        return switch (status) {
            case "READY" -> NotificationEvent.ready(userId);
            case "WAITING" -> NotificationEvent.waiting(userId);
            case "NOT_READY" -> NotificationEvent.notReady(userId);
            default -> NotificationEvent.notReady(userId);
        };
    }

    private void sendEvent(SseEmitter emitter, NotificationEvent event) {
        try {
            emitter.send(event);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        // Complete all active emitters
        emitters.values().forEach(SseEmitter::complete);
        scheduler.shutdown();
    }

    String getCurrentStatus(String userId) {
        return userStatuses.get(userId);
    }
}

