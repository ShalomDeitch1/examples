package com.example.ticketmaster.waitingroom.tokensession.store;

import com.example.ticketmaster.waitingroom.tokensession.model.TokenSession;
import com.example.ticketmaster.waitingroom.tokensession.model.TokenSessionStatus;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionStore implements SessionStore {
  // Session state is stored as a Redis hash per session.
  private static final String SESSION_KEY_PREFIX = "waiting-room:session:";

  // Active capacity is enforced using a Redis Set that contains ACTIVE session IDs.
  private static final String ACTIVE_SET_KEY = "waiting-room:active";

  private final StringRedisTemplate redis;

  public RedisSessionStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public void createWaiting(String sessionId, String eventId, String userId) {
    // Store enough metadata for debugging/teaching; the public API only returns {sessionId,status}.
    redis.opsForHash().putAll(
        sessionKey(sessionId),
        Map.of(
            "status", TokenSessionStatus.WAITING.name(),
            "eventId", eventId,
            "userId", userId
        )
    );
  }

  @Override
  public Optional<TokenSessionStatus> getStatus(String sessionId) {
    Object raw = redis.opsForHash().get(sessionKey(sessionId), "status");
    if (raw == null) {
      return Optional.empty();
    }
    return Optional.of(TokenSessionStatus.valueOf(raw.toString()));
  }

  @Override
  public void markActive(String sessionId) {
    // Status and the active-set must both be updated so capacity checks are fast.
    redis.opsForHash().put(sessionKey(sessionId), "status", TokenSessionStatus.ACTIVE.name());
    redis.opsForSet().add(ACTIVE_SET_KEY, sessionId);
  }

  @Override
  public void markLeft(String sessionId) {
    // Leaving releases capacity by removing the session from the active set.
    redis.opsForHash().put(sessionKey(sessionId), "status", TokenSessionStatus.LEFT.name());
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
  public Optional<TokenSession> get(String sessionId) {
    Map<Object, Object> map = redis.opsForHash().entries(sessionKey(sessionId));
    if (map == null || map.isEmpty()) {
      return Optional.empty();
    }

    Object rawStatus = map.get("status");
    if (rawStatus == null) {
      return Optional.empty();
    }

    TokenSessionStatus status = TokenSessionStatus.valueOf(rawStatus.toString());
    return Optional.of(new TokenSession(sessionId, status));
  }

  private String sessionKey(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }
}
