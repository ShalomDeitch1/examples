/**
 * Why this exists in this repo:
 * - In-memory record of "what was processed on each tick" so observability and tests can see batch/tick behavior.
 *
 * Real system notes:
 * - You’d publish metrics/traces (Prometheus/OpenTelemetry) or write audit events to an event stream; keeping full history in memory won’t scale.
 *
 * How it fits this example flow:
 * - Schedulers/pollers call {@code record(...)} after each tick; controllers expose it via /observability.
 */
package com.example.ticketmaster.waitingroom.core;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessingHistory {
  private final AtomicLong sequence = new AtomicLong(0);
  private final CopyOnWriteArrayList<ProcessingBatch> batches = new CopyOnWriteArrayList<>();

  public record ProcessingBatch(long batchNumber, Instant processedAt, List<String> requestIds) {
  }

  public void record(List<String> requestIds) {
    if (requestIds == null || requestIds.isEmpty()) {
      return;
    }
    long batchNumber = sequence.incrementAndGet();
    batches.add(new ProcessingBatch(batchNumber, Instant.now(), List.copyOf(requestIds)));
  }

  public List<ProcessingBatch> list() {
    return List.copyOf(batches);
  }
}
