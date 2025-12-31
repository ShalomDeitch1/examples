/**
 * Why this exists in this repo:
 * - Demonstrates RabbitMQ consumption in pull mode: a scheduled poll loop reads messages via {@code basicGet}.
 *
 * Real system notes:
 * - High-scale systems tune prefetch/QoS, retries, DLQs, and idempotency; work is usually recorded durably and updated in batches.
 * - This demo is intentionally in-memory (see {@code waiting-room-core}).
 *
 * How it fits this example flow:
 * - Pulls up to N messages per tick, marks request IDs processed in {@code RequestStore}, records a processing batch, and acks messages.
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.ProcessingProperties;
import com.example.ticketmaster.waitingroom.core.RequestStore;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PullGrantPoller {
  private static final Logger log = LoggerFactory.getLogger(PullGrantPoller.class);

  private final RabbitTemplate rabbitTemplate;
  private final RabbitPullProperties rabbit;
  private final RequestStore store;
  private final ProcessingProperties processing;
  private final ProcessingHistory processingHistory;
  private volatile boolean running = true;

  public PullGrantPoller(
      RabbitTemplate rabbitTemplate,
      RabbitPullProperties rabbit,
      RequestStore store,
      ProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.rabbitTemplate = rabbitTemplate;
    this.rabbit = rabbit;
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void pollAndProcess() {
    if (!running) {
      return;
    }

    try {
      int batchSize = processing.batchSize();
      if (batchSize <= 0) {
        return;
      }

      var processedIds = rabbitTemplate.execute(channel -> {
        var processed = new ArrayList<String>(batchSize);

        for (int i = 0; i < batchSize; i++) {
          var response = channel.basicGet(rabbit.queue(), false);
          if (response == null) {
            break;
          }

          String requestId = new String(response.getBody(), StandardCharsets.UTF_8);
          store.markProcessed(requestId);
          processed.add(requestId);

          channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
        }

        return processed;
      });

      if (processedIds != null && !processedIds.isEmpty()) {
        processingHistory.record(processedIds);
      }
    } catch (Exception e) {
      log.warn("Error in PullGrantPoller.pollAndProcess: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }
}
