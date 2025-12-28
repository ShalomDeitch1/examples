package com.example.ticketmaster.waitingroom.kafka;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class WaitingRoomStore {
  private final ConcurrentHashMap<String, WaitingRoomSession> sessions = new ConcurrentHashMap<>();
  private final Clock clock;

  public WaitingRoomStore() {
    this(Clock.systemUTC());
  }

  WaitingRoomStore(Clock clock) {
    this.clock = clock;
  }

  public WaitingRoomSession createWaiting(String eventId, String userId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId is required");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }

    Instant now = clock.instant();
    String id = UUID.randomUUID().toString();
    WaitingRoomSession session = new WaitingRoomSession(id, eventId, userId, WaitingRoomSessionStatus.WAITING, now, null, now);
    sessions.put(id, session);
    return session;
  }

  public Optional<WaitingRoomSession> get(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  public int activeCount() {
    return (int) sessions.values().stream().filter(s -> s.status() == WaitingRoomSessionStatus.ACTIVE).count();
  }

  public Collection<WaitingRoomSession> allSessions() {
    return sessions.values();
  }

  public boolean tryActivateIfCapacityAllows(String sessionId, int maxActive) {
    if (maxActive <= 0) {
      throw new IllegalArgumentException("maxActive must be > 0");
    }
    if (activeCount() >= maxActive) {
      return false;
    }
    WaitingRoomSession updated = activate(sessionId);
    return updated.status() == WaitingRoomSessionStatus.ACTIVE;
  }

  public WaitingRoomSession heartbeat(String sessionId) {
    Instant now = clock.instant();
    return sessions.compute(sessionId, (id, existing) -> {
      if (existing == null) {
        throw new IllegalArgumentException("Unknown session: " + sessionId);
      }
      if (existing.status() == WaitingRoomSessionStatus.EXPIRED) {
        return existing;
      }
      return new WaitingRoomSession(existing.id(), existing.eventId(), existing.userId(), existing.status(), existing.createdAt(), existing.activatedAt(), now);
    });
  }

  public WaitingRoomSession activate(String sessionId) {
    Instant now = clock.instant();
    return sessions.compute(sessionId, (id, existing) -> {
      if (existing == null) {
        throw new IllegalArgumentException("Unknown session: " + sessionId);
      }
      if (existing.status() == WaitingRoomSessionStatus.ACTIVE) {
        return existing;
      }
      if (existing.status() == WaitingRoomSessionStatus.EXPIRED) {
        return existing;
      }
      return new WaitingRoomSession(existing.id(), existing.eventId(), existing.userId(), WaitingRoomSessionStatus.ACTIVE, existing.createdAt(), now, existing.lastHeartbeatAt());
    });
  }

  public WaitingRoomSession expire(String sessionId) {
    return sessions.compute(sessionId, (id, existing) -> {
      if (existing == null) {
        throw new IllegalArgumentException("Unknown session: " + sessionId);
      }
      if (existing.status() == WaitingRoomSessionStatus.EXPIRED) {
        return existing;
      }
      return new WaitingRoomSession(existing.id(), existing.eventId(), existing.userId(), WaitingRoomSessionStatus.EXPIRED, existing.createdAt(), existing.activatedAt(), existing.lastHeartbeatAt());
    });
  }
}
