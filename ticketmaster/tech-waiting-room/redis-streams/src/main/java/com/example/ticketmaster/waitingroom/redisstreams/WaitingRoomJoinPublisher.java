package com.example.ticketmaster.waitingroom.redisstreams;

import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomJoinPublisher {
  private final StringRedisTemplate redis;
  private final RedisStreamsWaitingRoomProperties properties;

  public WaitingRoomJoinPublisher(StringRedisTemplate redis, RedisStreamsWaitingRoomProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  public void publishJoin(String sessionId, String eventId, String userId) {
    Map<String, String> payload = Map.of(
        "sessionId", sessionId,
        "eventId", eventId,
        "userId", userId
    );

    ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
        .ofObject(payload)
        .withStreamKey(properties.stream());

    redis.opsForStream().add(record);
  }
}
