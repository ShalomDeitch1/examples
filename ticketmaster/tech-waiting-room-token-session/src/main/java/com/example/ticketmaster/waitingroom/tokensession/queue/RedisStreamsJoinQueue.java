package com.example.ticketmaster.waitingroom.tokensession.queue;

import com.example.ticketmaster.waitingroom.tokensession.TokenSessionProperties;
import java.util.Map;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamsJoinQueue implements JoinQueue {
  private final StringRedisTemplate redis;
  private final TokenSessionProperties properties;

  public RedisStreamsJoinQueue(StringRedisTemplate redis, TokenSessionProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public void enqueue(String sessionId, String eventId, String userId) {
    redis.opsForStream()
        .add(
            StreamRecords.mapBacked(Map.of(
                "sessionId", sessionId,
                "eventId", eventId,
                "userId", userId
            )).withStreamKey(properties.redis().stream())
        );
  }
}
