/**
 * Why this exists in this repo:
 * - Tick-based batch processor for the RabbitMQ push-mode module.
 *
 * Real system notes:
 * - Production processors must be horizontally scalable and rely on durable state + idempotency; per-item DB updates don’t scale.
 *
 * How it fits this example flow:
 * - Drains up to N request IDs from the backlog per tick and marks them processed in the core store.
 */
package com.example.ticketmaster.waitingroom.rabbitmq;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.ProcessingProperties;
import com.example.ticketmaster.waitingroom.core.RequestStore;
import com.example.ticketmaster.waitingroom.core.push.GroupCollector;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PushGrantScheduler {
  private static final Logger log = LoggerFactory.getLogger(PushGrantScheduler.class);
  private final GroupCollector groupCollector;
  private final RequestStore store;
  private final ProcessingProperties processing;
  private final ProcessingHistory processingHistory;
  private volatile boolean running = true;

  public PushGrantScheduler(
      GroupCollector groupCollector,
      RequestStore store,
      ProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.groupCollector = groupCollector;
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void grantAvailable() {
    if (!running) {
      return;
    }
    try {
      int toProcess = processing.batchSize();
      var processedIds = new ArrayList<String>(toProcess);
      for (int i = 0; i < toProcess; i++) {
        var next = groupCollector.poll();
        if (next.isEmpty()) {
          break;
        }

        String requestId = next.get();
        store.markProcessed(requestId);
        processedIds.add(requestId);
      }

      processingHistory.record(processedIds);
    } catch (Exception e) {
      log.warn("Error in RabbitMQ PushGrantScheduler.polling: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }
}
