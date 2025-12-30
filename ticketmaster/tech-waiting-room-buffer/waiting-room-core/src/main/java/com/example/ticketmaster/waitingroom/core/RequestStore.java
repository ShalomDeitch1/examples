/**
 * Why this exists in this repo:
 * - Minimal in-memory "state store" for waiting-room requests (WAITING/PROCESSED) so the examples can focus on the pipe tech.
 *
 * Real system notes:
 * - This would be a durable store (DB/Redis) with idempotency keys and set-based/batched updates; per-item DB roundtrips won’t scale.
 * - You’d typically record processing outcomes in bulk and rely on constraints (unique keys) to tolerate duplicates.
 *
 * How it fits this example flow:
 * - HTTP creates requests here, schedulers/pollers mark them processed, and observability endpoints read counts/snapshots.
 */
package com.example.ticketmaster.waitingroom.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RequestStore {
  private final AtomicLong sequence = new AtomicLong(0);
  private final ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<>();
  private final Clock clock;

  public RequestStore() {
    this(Clock.systemUTC());
  }

  RequestStore(Clock clock) {
    this.clock = clock;
  }

  public Request createWaiting(String eventId, String userId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId is required");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }

    String id = Long.toString(sequence.incrementAndGet());
    Instant now = clock.instant();
    Request request = new Request(id, eventId, userId, RequestStatus.WAITING, now, null);
    requests.put(id, request);
    return request;
  }

  public Optional<Request> get(String requestId) {
    return Optional.ofNullable(requests.get(requestId));
  }

  public boolean markProcessed(String requestId) {
    Request updated = requests.computeIfPresent(requestId, (id, existing) -> {
      if (existing.status() == RequestStatus.PROCESSED) {
        return existing;
      }
      Instant now = clock.instant();
      return new Request(existing.id(), existing.eventId(), existing.userId(), RequestStatus.PROCESSED, existing.createdAt(), now);
    });

    return updated != null;
  }

  public Counts counts() {
    long waiting = requests.values().stream().filter(r -> r.status() == RequestStatus.WAITING).count();
    long processed = requests.values().stream().filter(r -> r.status() == RequestStatus.PROCESSED).count();
    return new Counts(waiting, processed, requests.size());
  }

  public record Counts(long waiting, long processed, long total) {
  }

  public Map<String, Request> snapshot() {
    return Map.copyOf(requests);
  }
}
