package com.example.ticketmaster.waitingroom.tokensession.queue;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamCursorStore implements StreamCursorStore {
  // Stores the last processed stream entry ID so the scheduler can continue where it left off.
  // This is a deliberately minimal alternative to consumer groups for teaching purposes.
  private static final String LAST_ID_KEY = "waiting-room:stream:last-id";

  private final StringRedisTemplate redis;

  public RedisStreamCursorStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public String getLastId() {
    String value = redis.opsForValue().get(LAST_ID_KEY);
    // "0-0" means "start from the beginning".
    return value == null || value.isBlank() ? "0-0" : value;
  }

  @Override
  public void setLastId(String recordId) {
    redis.opsForValue().set(LAST_ID_KEY, recordId);
  }
}
