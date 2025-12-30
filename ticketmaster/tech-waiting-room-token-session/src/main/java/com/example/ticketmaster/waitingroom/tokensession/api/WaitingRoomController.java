package com.example.ticketmaster.waitingroom.tokensession.api;

import com.example.ticketmaster.waitingroom.tokensession.model.WaitingRoomSession;
import com.example.ticketmaster.waitingroom.tokensession.store.SessionStore;
import com.example.ticketmaster.waitingroom.tokensession.queue.JoinQueue;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/waiting-room")
public class WaitingRoomController {
  private final SessionStore store;
  private final JoinQueue queue;

  public WaitingRoomController(SessionStore store, JoinQueue queue) {
    this.store = store;
    this.queue = queue;
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CreateSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
    String sessionId = UUID.randomUUID().toString();
    store.createWaiting(sessionId, request.eventId(), request.userId());
    queue.enqueue(sessionId, request.eventId(), request.userId());
    return new CreateSessionResponse(sessionId);
  }

  @GetMapping("/sessions/{sessionId}")
  public WaitingRoomSession get(@PathVariable String sessionId) {
    return store.get(sessionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/sessions/{sessionId}:leave")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(@PathVariable String sessionId) {
    if (store.getStatus(sessionId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    store.markLeft(sessionId);
  }
}
