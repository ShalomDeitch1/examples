/**
 * Why this exists in this repo:
 * - Demonstrates Kafka consumption in pull mode: a scheduled poll loop processes up to N records per tick.
 *
 * Real system notes:
 * - High-scale consumers handle rebalances, retries/backoff, poison messages, back-pressure, and careful commit strategies.
 * - This demo runs in-memory; real systems persist state and use idempotency keys/unique constraints.
 *
 * How it fits this example flow:
 * - Polls Kafka for request IDs, marks them processed in {@code RequestStore}, records a batch, and commits offsets for processed records.
 */
package com.example.ticketmaster.waitingroom.kafka.pull;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.ProcessingProperties;
import com.example.ticketmaster.waitingroom.core.RequestStore;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PullGrantPoller {
  private static final Logger log = LoggerFactory.getLogger(PullGrantPoller.class);

  private final RequestStore store;
  private final ProcessingProperties processing;
  private final ProcessingHistory processingHistory;
  private final Consumer<String, String> consumer;
  private final Duration pollTimeout = Duration.ofMillis(200);

  private volatile boolean running = true;

  /**
   * Why this exists:
   * - Creates a dedicated Kafka consumer for this demo poller and subscribes it to the join topic.
   *
   * Real system notes:
   * - Group ID should be explicitly configured and validated at startup.
   * - Larger systems usually rely on a managed listener container, or they centralize consumer lifecycle (threads,
   *   rebalances, shutdown hooks) rather than constructing a consumer in a component constructor.
   */
  public PullGrantPoller(
      ConsumerFactory<String, String> consumerFactory,
      KafkaProperties kafkaProperties,
      KafkaPullProperties kafka,
      RequestStore store,
      ProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;

    String groupId = kafkaProperties.getConsumer().getGroupId();
    if (groupId == null || groupId.isBlank()) {
      // Demo-friendly fallback. In production this should be configured explicitly and validated at startup.
      groupId = "waiting-room-granter";
    }

    this.consumer = consumerFactory.createConsumer(groupId, "waiting-room-granter", null);
    this.consumer.subscribe(List.of(kafka.topic()));
  }

  /**
   * Why this exists:
   * - Represents the core of the "pull" example: on each fixed tick, poll Kafka, process up to N records,
   *   then commit offsets for only the records processed in this tick.
   *
   * How it fits the flow:
   * - Kafka is the pipe; this scheduled method is the batch processor.
   * - {@link RequestStore} is updated so the HTTP observability endpoint can report progress.
   * - {@link ProcessingHistory} is updated so tests can assert batch size and tick behavior.
   *
   * Real system notes:
   * - Production consumers typically implement robust error handling (retries/backoff/DLQ), idempotency, and
   *   carefully chosen commit semantics.
   */
  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void pollAndProcess() {
    if (!running) {
      return;
    }

    try {
      ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
      int batchSize = processing.batchSize();

      if (records.isEmpty() || batchSize <= 0) {
        return;
      }

      List<String> processedIds = new ArrayList<>(Math.min(batchSize, records.count()));
      Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

      int processed = 0;

      Map<TopicPartition, Long> firstSeenOffsetByPartition = firstSeenOffsetByPartition(records);
      Map<TopicPartition, Long> lastProcessedOffsetByPartition = new HashMap<>();

      for (ConsumerRecord<String, String> record : records) {
        if (processed >= batchSize) {
          break;
        }

        String requestId = record.value();
        store.markProcessed(requestId);
        processedIds.add(requestId);

        offsetsToCommit.put(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1)
        );

        lastProcessedOffsetByPartition.put(new TopicPartition(record.topic(), record.partition()), record.offset());

        processed++;
      }

      if (!processedIds.isEmpty()) {
        processingHistory.record(processedIds);
        consumer.commitSync(offsetsToCommit);
      }

      rewindUnprocessed(records.partitions(), firstSeenOffsetByPartition, lastProcessedOffsetByPartition);
    } catch (Exception e) {
      log.warn("Error in PullGrantPoller.pollAndProcess: {}", e.toString());
    }
  }

  /**
   * Why this exists:
   * - Captures the first offset observed per partition in the current poll() result.
   *
   * How it fits this example:
   * - This poller may process only a subset of records returned by poll() (batch size N).
   * - By remembering the first seen offset and the last processed offset, we can compute where to seek for the
   *   next tick so we don't lose unprocessed records.
   *
   * Real system notes:
   * - Many production designs avoid manual seek management by controlling max-poll-records and using commits,
   *   or by using frameworks that manage consumer state.
   */
  private static Map<TopicPartition, Long> firstSeenOffsetByPartition(ConsumerRecords<String, String> records) {
    Map<TopicPartition, Long> firstOffsets = new HashMap<>();
    for (TopicPartition tp : records.partitions()) {
      List<ConsumerRecord<String, String>> partitionRecords = records.records(tp);
      if (!partitionRecords.isEmpty()) {
        firstOffsets.put(tp, partitionRecords.getFirst().offset());
      }
    }
    return firstOffsets;
  }

  /**
   * Why this exists:
   * - Ensures that any records returned by poll() but not processed in this tick will be re-read on the next tick.
   *
   * How it fits this example:
   * - We poll more than we process (Kafka returns up to max-poll-records); the demo wants "process up to N per tick".
   * - After committing offsets for processed records, we explicitly seek to the next unprocessed offset.
   *
   * Real system notes:
   * - Manual seek management can interact with rebalances and error handling; production consumers should be
   *   very deliberate about seek/commit strategies.
   */
  private void rewindUnprocessed(
      Set<TopicPartition> partitions,
      Map<TopicPartition, Long> firstSeenOffsetByPartition,
      Map<TopicPartition, Long> lastProcessedOffsetByPartition
  ) {
    for (TopicPartition tp : partitions) {
      Long firstSeen = firstSeenOffsetByPartition.get(tp);
      if (firstSeen == null) {
        continue;
      }

      Long lastProcessed = lastProcessedOffsetByPartition.get(tp);
      long nextOffsetToProcess = lastProcessed == null ? firstSeen : lastProcessed + 1;
      consumer.seek(tp, nextOffsetToProcess);
    }
  }

  /**
   * Why this exists:
   * - Stops the scheduled loop and closes the Kafka consumer so tests and local runs shutdown cleanly.
   *
   * Real system notes:
   * - Production shutdown is usually coordinated (stop polling, commit final offsets if appropriate, and close).
   */
  @PreDestroy
  public void shutdown() {
    running = false;
    try {
      consumer.close();
    } catch (Exception ignored) {
      // best-effort
    }
  }
}
