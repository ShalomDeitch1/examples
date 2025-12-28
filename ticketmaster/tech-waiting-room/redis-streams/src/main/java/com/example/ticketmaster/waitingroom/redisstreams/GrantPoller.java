package com.example.ticketmaster.waitingroom.redisstreams;

import java.util.List;
import java.util.Map;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantPoller {
  private final StringRedisTemplate redis;
  private final RedisStreamsWaitingRoomProperties properties;
  private final WaitingRoomStore store;
  private final WaitingRoomCapacityProperties capacity;

  public GrantPoller(
      StringRedisTemplate redis,
      RedisStreamsWaitingRoomProperties properties,
      WaitingRoomStore store,
      WaitingRoomCapacityProperties capacity
  ) {
    this.redis = redis;
    this.properties = properties;
    this.store = store;
    this.capacity = capacity;
  }

  @Scheduled(fixedDelayString = "${waitingroom.grant.rate-ms:200}")
  public void pollAndGrant() {
    if (store.activeCount() >= capacity.maxActive()) {
      return;
    }

    Consumer consumer = Consumer.from(properties.consumerGroup(), properties.consumerName());
    StreamReadOptions options = StreamReadOptions.empty().count(1);

    List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
        consumer,
        options,
        StreamOffset.create(properties.stream(), ReadOffset.lastConsumed())
    );

    if (records == null || records.isEmpty()) {
      return;
    }

    MapRecord<String, Object, Object> record = records.getFirst();
    Map<Object, Object> value = record.getValue();
    Object rawSessionId = value.get("sessionId");
    String sessionId = rawSessionId == null ? null : rawSessionId.toString();
    if (sessionId == null || sessionId.isBlank()) {
      redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
      return;
    }

    boolean activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
    if (activated) {
      redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
    }
  }
}
