package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantScheduler {
  private final JoinBacklog backlog;
  private final WaitingRoomStore store;
  private final WaitingRoomCapacityProperties capacity;

  public GrantScheduler(JoinBacklog backlog, WaitingRoomStore store, WaitingRoomCapacityProperties capacity) {
    this.backlog = backlog;
    this.store = store;
    this.capacity = capacity;
  }

  @Scheduled(fixedDelayString = "${waitingroom.grant.rate-ms:200}")
  public void grantNext() {
    backlog.poll().ifPresent(sessionId -> {
      boolean activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
      if (!activated) {
        backlog.enqueue(sessionId);
      }
    });
  }
}
