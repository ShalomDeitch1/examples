package com.example.ticketmaster.waitingroom.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ProcessingBatcher {
  private final int batchSize;
  private final ProcessingHistory history;
  private final LinkedList<String> buffer = new LinkedList<>();

  public ProcessingBatcher(int batchSize, ProcessingHistory history) {
    this.batchSize = batchSize;
    this.history = history;
  }

  public synchronized void add(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    buffer.addAll(ids);
    flushFullBatches();
  }

  public synchronized void add(String id) {
    if (id == null) {
      return;
    }
    buffer.add(id);
    flushFullBatches();
  }

  private void flushFullBatches() {
    while (buffer.size() >= batchSize) {
      List<String> batch = new ArrayList<>(batchSize);
      for (int i = 0; i < batchSize; i++) {
        batch.add(buffer.removeFirst());
      }
      history.record(batch);
    }
  }

  /**
   * Flush any remaining buffered items as a final (partial) batch.
   */
  public synchronized void flushRemaining() {
    if (buffer.isEmpty()) {
      return;
    }
    List<String> batch = new ArrayList<>(buffer);
    buffer.clear();
    history.record(batch);
  }
}
