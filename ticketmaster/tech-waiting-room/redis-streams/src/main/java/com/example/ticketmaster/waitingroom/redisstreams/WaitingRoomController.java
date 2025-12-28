package com.example.ticketmaster.waitingroom.redisstreams;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomObservability;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequest;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequestStore;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WaitingRoomController {
  private final WaitingRoomRequestStore store;
  private final WaitingRoomJoinPublisher publisher;
  private final ProcessingHistory processingHistory;

  public WaitingRoomController(
      WaitingRoomRequestStore store,
      WaitingRoomJoinPublisher publisher,
      ProcessingHistory processingHistory
  ) {
    this.store = store;
    this.publisher = publisher;
    this.processingHistory = processingHistory;
  }

  public record EnqueueRequest(String eventId, String userId) {
  }

  @PostMapping("/api/waiting-room/requests")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> enqueue(@RequestBody EnqueueRequest request) {
    WaitingRoomRequest queued = store.createWaiting(request.eventId(), request.userId());
    publisher.publishRequest(queued.id());
    return Map.of("requestId", queued.id());
  }

  @GetMapping("/api/waiting-room/observability")
  public WaitingRoomObservability observability() {
    return new WaitingRoomObservability(store.counts(), processingHistory.list());
  }
}

