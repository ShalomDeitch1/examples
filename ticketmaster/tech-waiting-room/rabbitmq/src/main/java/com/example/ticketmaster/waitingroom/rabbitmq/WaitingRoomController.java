package com.example.ticketmaster.waitingroom.rabbitmq;

import java.util.Map;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WaitingRoomController {
  private final WaitingRoomStore store;
  private final WaitingRoomJoinPublisher joinPublisher;
  private final GrantHistory grantHistory;

  public WaitingRoomController(WaitingRoomStore store, WaitingRoomJoinPublisher joinPublisher, GrantHistory grantHistory) {
    this.store = store;
    this.joinPublisher = joinPublisher;
    this.grantHistory = grantHistory;
  }

  public record JoinRequest(String eventId, String userId) {
  }

  @PostMapping("/api/waiting-room/sessions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> join(@RequestBody JoinRequest request) {
    WaitingRoomSession session = store.createWaiting(request.eventId(), request.userId());
    joinPublisher.publishJoin(session.id());
    return Map.of("sessionId", session.id());
  }

  @GetMapping("/api/waiting-room/sessions/{id}")
  public WaitingRoomSession status(@PathVariable("id") String id) {
    return store.get(id).orElseThrow(() -> new IllegalArgumentException("Unknown session: " + id));
  }

  @GetMapping("/api/waiting-room/grant-batches")
  public List<GrantHistory.GrantBatch> grantBatches() {
    return grantHistory.list();
  }

  @PostMapping("/api/waiting-room/sessions/{id}:heartbeat")
  public WaitingRoomSession heartbeat(@PathVariable("id") String id) {
    return store.heartbeat(id);
  }

  @PostMapping("/api/waiting-room/sessions/{id}:leave")
  public WaitingRoomSession leave(@PathVariable("id") String id) {
    return store.expire(id);
  }
}

