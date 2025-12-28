package com.example.ticketmaster.waitingroom.kafka;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomObservability;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequest;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequestStore;

@RestController
public class WaitingRoomController {
  private final WaitingRoomRequestStore store;
  private final WaitingRoomJoinPublisher joinPublisher;
  private final ProcessingHistory processingHistory;

  public WaitingRoomController(WaitingRoomRequestStore store, WaitingRoomJoinPublisher joinPublisher, ProcessingHistory processingHistory) {
    this.store = store;
    this.joinPublisher = joinPublisher;
    this.processingHistory = processingHistory;
  }

  public record EnqueueRequest(String eventId, String userId) {
  }

  @PostMapping("/api/waiting-room/requests")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> enqueue(@RequestBody EnqueueRequest request) {
    WaitingRoomRequest queued = store.createWaiting(request.eventId(), request.userId());
    joinPublisher.publishRequest(queued.eventId(), queued.id());
    return Map.of("requestId", queued.id());
  }

  @GetMapping("/api/waiting-room/observability")
  public WaitingRoomObservability observability() {
    return new WaitingRoomObservability(store.counts(), processingHistory.list());
  }
}

