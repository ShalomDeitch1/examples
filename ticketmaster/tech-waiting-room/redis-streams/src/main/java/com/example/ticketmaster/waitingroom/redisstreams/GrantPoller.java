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
  private final GrantHistory grantHistory;
  private final WaitingRoomGrantProperties grant;

  public GrantPoller(
      StringRedisTemplate redis,
      RedisStreamsWaitingRoomProperties properties,
      WaitingRoomStore store,
      WaitingRoomCapacityProperties capacity,
      GrantHistory grantHistory,
      WaitingRoomGrantProperties grant
  ) {
    this.redis = redis;
    this.properties = properties;
    this.store = store;
    this.capacity = capacity;
    this.grantHistory = grantHistory;
    this.grant = grant;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.grant.rate-ms:200}",
      initialDelayString = "${waitingroom.grant.initial-delay-ms:0}"
  )
  public void pollAndGrant() {
    int availableSlots = capacity.maxActive() - store.activeCount();
    if (availableSlots <= 0) {
      return;
    }

    int toGrant = Math.min(availableSlots, grant.groupSize());

    Consumer consumer = Consumer.from(properties.consumerGroup(), properties.consumerName());
    StreamReadOptions options = StreamReadOptions.empty().count(toGrant);

    List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
        consumer,
        options,
        StreamOffset.create(properties.stream(), ReadOffset.lastConsumed())
    );

    if (records == null || records.isEmpty()) {
      return;
    }

    var activatedIds = new java.util.ArrayList<String>(toGrant);
    for (MapRecord<String, Object, Object> record : records) {
      Map<Object, Object> value = record.getValue();
      Object rawSessionId = value.get("sessionId");
      String sessionId = rawSessionId == null ? null : rawSessionId.toString();
      if (sessionId == null || sessionId.isBlank()) {
        redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
        continue;
      }

      boolean activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
      if (activated) {
        activatedIds.add(sessionId);
        redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
        continue;
      }

      // Capacity race: ack, then re-enqueue for later to avoid leaving the message pending forever.
      redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
      redis.opsForStream().add(org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
          .ofObject(java.util.Map.of("sessionId", sessionId))
          .withStreamKey(properties.stream()));
      break;
    }

    grantHistory.record(activatedIds);
  }
}

