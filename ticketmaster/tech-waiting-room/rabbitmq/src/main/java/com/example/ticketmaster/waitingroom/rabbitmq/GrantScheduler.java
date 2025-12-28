package com.example.ticketmaster.waitingroom.rabbitmq;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomProcessingProperties;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequestStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantScheduler {
  private final JoinBacklog backlog;
  private final WaitingRoomRequestStore store;
  private final WaitingRoomProcessingProperties processing;
  private final ProcessingHistory processingHistory;

  public GrantScheduler(
      JoinBacklog backlog,
      WaitingRoomRequestStore store,
      WaitingRoomProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.backlog = backlog;
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void grantAvailable() {
    int toProcess = processing.batchSize();
    var processedIds = new java.util.ArrayList<String>(toProcess);
    for (int i = 0; i < toProcess; i++) {
      var next = backlog.poll();
      if (next.isEmpty()) {
        break;
      }

      String requestId = next.get();
      store.markProcessed(requestId);
      processedIds.add(requestId);
    }

    processingHistory.record(processedIds);
  }
}

