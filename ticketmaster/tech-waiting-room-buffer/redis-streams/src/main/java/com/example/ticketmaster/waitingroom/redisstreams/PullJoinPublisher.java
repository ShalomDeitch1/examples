package com.example.ticketmaster.waitingroom.redisstreams;

import java.util.Map;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PullJoinPublisher {
  private final StringRedisTemplate redis;
  private final RedisStreamsProperties properties;

  public PullJoinPublisher(StringRedisTemplate redis, RedisStreamsProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  public void publishRequest(String requestId) {
    Map<String, String> payload = Map.of("requestId", requestId);

    ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
        .ofObject(payload)
        .withStreamKey(properties.stream());

    redis.opsForStream().add(record);
  }
}
