package com.example.ticketmaster.waitingroom.redisstreams;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomProcessingProperties;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequestStore;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log = LoggerFactory.getLogger(GrantPoller.class);
  private final StringRedisTemplate redis;
  private final RedisStreamsWaitingRoomProperties properties;
  private final WaitingRoomRequestStore store;
  private final WaitingRoomProcessingProperties processing;
  private final ProcessingHistory processingHistory;
  private volatile boolean running = true;

  public GrantPoller(
      StringRedisTemplate redis,
      RedisStreamsWaitingRoomProperties properties,
      WaitingRoomRequestStore store,
      WaitingRoomProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.redis = redis;
    this.properties = properties;
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void pollAndGrant() {
    if (!running) {
      return;
    }
    try {
      int toProcess = processing.batchSize();

      Consumer consumer = Consumer.from(properties.consumerGroup(), properties.consumerName());
      StreamReadOptions options = StreamReadOptions.empty().count(toProcess);

      List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
          consumer,
          options,
          StreamOffset.create(properties.stream(), ReadOffset.lastConsumed())
      );

      if (records == null || records.isEmpty()) {
        return;
      }

      var processedIds = new java.util.ArrayList<String>(toProcess);
      for (MapRecord<String, Object, Object> record : records) {
        Map<Object, Object> value = record.getValue();
        Object rawRequestId = value.get("requestId");
        String requestId = rawRequestId == null ? null : rawRequestId.toString();
        if (requestId == null || requestId.isBlank()) {
          redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
          continue;
        }

        store.markProcessed(requestId);
        processedIds.add(requestId);
        redis.opsForStream().acknowledge(properties.stream(), properties.consumerGroup(), record.getId());
      }

      processingHistory.record(processedIds);
    } catch (Exception e) {
      if (!running) {
        return;
      }
      log.debug("Redis Streams poll failed: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }
}

