/**
 * Why this exists in this repo:
 * - Exposes the canonical HTTP API for the Kafka module: enqueue requests + read observability.
 *
 * Real system notes:
 * - Public APIs usually sit behind auth/rate limiting and return stable IDs; observability is usually metrics/tracing rather than an endpoint.
 *
 * How it fits this example flow:
 * - POST enqueues (publisher → Kafka); GET reads from core state (RequestStore + ProcessingHistory).
 */
package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.Observability;
import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.Request;
import com.example.ticketmaster.waitingroom.core.RequestStore;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PushController {
  private final RequestStore store;
  private final PushJoinPublisher joinPublisher;
  private final ProcessingHistory processingHistory;

  public PushController(RequestStore store, PushJoinPublisher joinPublisher, ProcessingHistory processingHistory) {
    this.store = store;
    this.joinPublisher = joinPublisher;
    this.processingHistory = processingHistory;
  }

  public record EnqueueRequest(String eventId, String userId) {
  }

  @PostMapping("/api/waiting-room/requests")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> enqueue(@RequestBody EnqueueRequest request) {
    Request queued = store.createWaiting(request.eventId(), request.userId());
    joinPublisher.publishRequest(queued.eventId(), queued.id());
    return Map.of("requestId", queued.id());
  }

  @GetMapping("/api/waiting-room/observability")
  public Observability observability() {
    return new Observability(store.counts(), processingHistory.list());
  }
}
