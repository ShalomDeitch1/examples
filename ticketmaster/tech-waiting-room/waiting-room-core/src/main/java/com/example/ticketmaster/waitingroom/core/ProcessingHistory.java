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
