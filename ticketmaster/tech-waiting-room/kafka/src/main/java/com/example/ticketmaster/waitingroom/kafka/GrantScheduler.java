package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantScheduler {
  private final JoinBacklog backlog;
  private final WaitingRoomStore store;
  private final WaitingRoomCapacityProperties capacity;
  private final GrantHistory grantHistory;
  private final WaitingRoomGrantProperties grant;

  public GrantScheduler(
      JoinBacklog backlog,
      WaitingRoomStore store,
      WaitingRoomCapacityProperties capacity,
      GrantHistory grantHistory,
      WaitingRoomGrantProperties grant
  ) {
    this.backlog = backlog;
    this.store = store;
    this.capacity = capacity;
    this.grantHistory = grantHistory;
    this.grant = grant;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.grant.rate-ms:200}",
      initialDelayString = "${waitingroom.grant.initial-delay-ms:0}"
  )
  public void grantAvailable() {
    int availableSlots = capacity.maxActive() - store.activeCount();
    if (availableSlots <= 0) {
      return;
    }

    int toGrant = Math.min(availableSlots, grant.groupSize());
    var activatedIds = new java.util.ArrayList<String>(toGrant);
    for (int i = 0; i < toGrant; i++) {
      var next = backlog.poll();
      if (next.isEmpty()) {
        break;
      }
      String sessionId = next.get();
      boolean activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
      if (!activated) {
        backlog.enqueue(sessionId);
        break;
      }
      activatedIds.add(sessionId);
    }

    grantHistory.record(activatedIds);
  }
}

