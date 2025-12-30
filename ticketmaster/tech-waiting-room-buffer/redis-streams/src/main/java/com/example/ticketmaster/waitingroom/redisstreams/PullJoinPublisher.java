/**
 * Why this exists in this repo:
 * - Appends join messages to Redis Streams (the pipe) for the pull-mode example.
 *
 * Real system notes:
 * - Production publishing includes schema/versioning, retention management, and back-pressure handling.
 *
 * How it fits this example flow:
 * - Called by the controller to write the request ID into the stream.
 */
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
