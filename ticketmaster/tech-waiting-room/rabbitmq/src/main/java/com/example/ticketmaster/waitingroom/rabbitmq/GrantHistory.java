package com.example.ticketmaster.waitingroom.rabbitmq;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class GrantHistory {
  private final AtomicLong sequence = new AtomicLong(0);
  private final CopyOnWriteArrayList<GrantBatch> batches = new CopyOnWriteArrayList<>();

  public record GrantBatch(long groupId, Instant grantedAt, List<String> sessionIds) {
  }

  public void record(List<String> sessionIds) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return;
    }
    long groupId = sequence.incrementAndGet();
    batches.add(new GrantBatch(groupId, Instant.now(), List.copyOf(sessionIds)));
  }

  public List<GrantBatch> list() {
    return List.copyOf(batches);
  }
}
