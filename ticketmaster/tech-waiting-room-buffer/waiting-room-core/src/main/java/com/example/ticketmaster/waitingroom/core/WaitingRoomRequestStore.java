package com.example.ticketmaster.waitingroom.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WaitingRoomRequestStore {
  private final AtomicLong sequence = new AtomicLong(0);
  private final ConcurrentHashMap<String, WaitingRoomRequest> requests = new ConcurrentHashMap<>();
  private final Clock clock;

  public WaitingRoomRequestStore() {
    this(Clock.systemUTC());
  }

  WaitingRoomRequestStore(Clock clock) {
    this.clock = clock;
  }

  public WaitingRoomRequest createWaiting(String eventId, String userId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId is required");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }

    String id = Long.toString(sequence.incrementAndGet());
    Instant now = clock.instant();
    WaitingRoomRequest request = new WaitingRoomRequest(id, eventId, userId, WaitingRoomRequestStatus.WAITING, now, null);
    requests.put(id, request);
    return request;
  }

  public Optional<WaitingRoomRequest> get(String requestId) {
    return Optional.ofNullable(requests.get(requestId));
  }

  public boolean markProcessed(String requestId) {
    WaitingRoomRequest updated = requests.computeIfPresent(requestId, (id, existing) -> {
      if (existing.status() == WaitingRoomRequestStatus.PROCESSED) {
        return existing;
      }
      Instant now = clock.instant();
      return new WaitingRoomRequest(existing.id(), existing.eventId(), existing.userId(), WaitingRoomRequestStatus.PROCESSED, existing.createdAt(), now);
    });

    return updated != null;
  }

  public WaitingRoomCounts counts() {
    long waiting = requests.values().stream().filter(r -> r.status() == WaitingRoomRequestStatus.WAITING).count();
    long processed = requests.values().stream().filter(r -> r.status() == WaitingRoomRequestStatus.PROCESSED).count();
    return new WaitingRoomCounts(waiting, processed, requests.size());
  }

  public record WaitingRoomCounts(long waiting, long processed, long total) {
  }

  public Map<String, WaitingRoomRequest> snapshot() {
    return Map.copyOf(requests);
  }
}
