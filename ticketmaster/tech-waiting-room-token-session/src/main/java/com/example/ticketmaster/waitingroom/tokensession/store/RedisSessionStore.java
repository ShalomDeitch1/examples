package com.example.ticketmaster.waitingroom.tokensession.store;

import com.example.ticketmaster.waitingroom.tokensession.model.WaitingRoomSession;
import com.example.ticketmaster.waitingroom.tokensession.model.WaitingRoomSessionStatus;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionStore implements SessionStore {
  private static final String SESSION_KEY_PREFIX = "waiting-room:session:";
  private static final String ACTIVE_SET_KEY = "waiting-room:active";

  private final StringRedisTemplate redis;

  public RedisSessionStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public void createWaiting(String sessionId, String eventId, String userId) {
    redis.opsForHash().putAll(
        sessionKey(sessionId),
        Map.of(
            "status", WaitingRoomSessionStatus.WAITING.name(),
            "eventId", eventId,
            "userId", userId
        )
    );
  }

  @Override
  public Optional<WaitingRoomSessionStatus> getStatus(String sessionId) {
    Object raw = redis.opsForHash().get(sessionKey(sessionId), "status");
    if (raw == null) {
      return Optional.empty();
    }
    return Optional.of(WaitingRoomSessionStatus.valueOf(raw.toString()));
  }

  @Override
  public void markActive(String sessionId) {
    redis.opsForHash().put(sessionKey(sessionId), "status", WaitingRoomSessionStatus.ACTIVE.name());
    redis.opsForSet().add(ACTIVE_SET_KEY, sessionId);
  }

  @Override
  public void markLeft(String sessionId) {
    redis.opsForHash().put(sessionKey(sessionId), "status", WaitingRoomSessionStatus.LEFT.name());
    redis.opsForSet().remove(ACTIVE_SET_KEY, sessionId);
  }

  @Override
  public boolean isActive(String sessionId) {
    Boolean isMember = redis.opsForSet().isMember(ACTIVE_SET_KEY, sessionId);
    return Boolean.TRUE.equals(isMember);
  }

  @Override
  public int activeCount() {
    Long size = redis.opsForSet().size(ACTIVE_SET_KEY);
    return size == null ? 0 : size.intValue();
  }

  @Override
  public Optional<WaitingRoomSession> get(String sessionId) {
    Map<Object, Object> map = redis.opsForHash().entries(sessionKey(sessionId));
    if (map == null || map.isEmpty()) {
      return Optional.empty();
    }

    Object rawStatus = map.get("status");
    if (rawStatus == null) {
      return Optional.empty();
    }

    WaitingRoomSessionStatus status = WaitingRoomSessionStatus.valueOf(rawStatus.toString());
    return Optional.of(new WaitingRoomSession(sessionId, status));
  }

  private String sessionKey(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }
}
