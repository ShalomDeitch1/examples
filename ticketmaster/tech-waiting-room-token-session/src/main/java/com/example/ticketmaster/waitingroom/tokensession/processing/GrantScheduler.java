package com.example.ticketmaster.waitingroom.tokensession.processing;

import com.example.ticketmaster.waitingroom.tokensession.TokenSessionProperties;
import com.example.ticketmaster.waitingroom.tokensession.queue.StreamCursorStore;
import com.example.ticketmaster.waitingroom.tokensession.store.SessionStore;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantScheduler {
  private static final Logger log = LoggerFactory.getLogger(GrantScheduler.class);

  private final StringRedisTemplate redis;
  private final TokenSessionProperties properties;
  private final SessionStore store;
  private final StreamCursorStore cursorStore;
  private volatile boolean running = true;

  public GrantScheduler(
      StringRedisTemplate redis,
      TokenSessionProperties properties,
      SessionStore store,
      StreamCursorStore cursorStore
  ) {
    this.redis = redis;
    this.properties = properties;
    this.store = store;
    this.cursorStore = cursorStore;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void grantAvailable() {
    // This is the core of the token/session waiting room:
    // - Read join events from the Redis Stream.
    // - While below capacity, mark sessions ACTIVE.
    // - Persist a cursor so subsequent ticks continue from the last processed entry.
    if (!running) {
      return;
    }
    try {
      int capacity = properties.processing().capacity();
      int batchSize = properties.processing().batchSize();

      String lastId = cursorStore.getLastId();
      List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
          StreamReadOptions.empty().count(batchSize),
          StreamOffset.create(properties.redis().stream(), ReadOffset.from(lastId))
      );

      if (records == null || records.isEmpty()) {
        return;
      }

      String lastProcessedId = null;
      for (MapRecord<String, Object, Object> record : records) {
        // Enforce the rule: never exceed configured ACTIVE capacity.
        if (store.activeCount() >= capacity) {
          break;
        }

        Map<Object, Object> value = record.getValue();
        Object rawSessionId = value.get("sessionId");
        String sessionId = rawSessionId == null ? null : rawSessionId.toString();
        if (sessionId == null || sessionId.isBlank()) {
          // Malformed record: skip it but still advance the cursor so we don't get stuck.
          lastProcessedId = record.getId().getValue();
          continue;
        }

        if (!store.isActive(sessionId)) {
          // Idempotent activation (safe if the same session is processed again).
          store.markActive(sessionId);
        }
        lastProcessedId = record.getId().getValue();
      }

      if (lastProcessedId != null) {
        cursorStore.setLastId(lastProcessedId);
      }
    } catch (Exception e) {
      if (!running) {
        return;
      }
      // Keep the teaching example resilient: one bad tick shouldn't kill the app.
      log.debug("Grant scheduler tick failed: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }

}
