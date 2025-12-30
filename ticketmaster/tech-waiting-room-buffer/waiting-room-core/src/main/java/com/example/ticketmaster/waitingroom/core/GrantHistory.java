package com.example.ticketmaster.waitingroom.core;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class GrantHistory {
  private final AtomicLong sequence = new AtomicLong(0);
  private final CopyOnWriteArrayList<GrantBatch> batches = new CopyOnWriteArrayList<>();

  public record GrantBatch(long batchNumber, Instant grantedAt, List<String> sessionIds) {
  }

  public void record(List<String> sessionIds) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return;
    }
    long batchNumber = sequence.incrementAndGet();
    batches.add(new GrantBatch(batchNumber, Instant.now(), List.copyOf(sessionIds)));
  }

  public List<GrantBatch> list() {
    return List.copyOf(batches);
  }
}
