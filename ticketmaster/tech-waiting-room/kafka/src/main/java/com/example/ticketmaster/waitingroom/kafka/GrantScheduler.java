package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.ProcessingBatcher;
import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomProcessingProperties;
import com.example.ticketmaster.waitingroom.core.WaitingRoomRequestStore;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GrantScheduler {
  private static final Logger log = LoggerFactory.getLogger(GrantScheduler.class);
  private final JoinBacklog backlog;
  private final WaitingRoomRequestStore store;
  private final WaitingRoomProcessingProperties processing;
  private final ProcessingBatcher batcher;
  private final ProcessingHistory processingHistory;
  private volatile boolean running = true;

  public GrantScheduler(
      JoinBacklog backlog,
      WaitingRoomRequestStore store,
      WaitingRoomProcessingProperties processing,
      ProcessingBatcher batcher,
      ProcessingHistory processingHistory
  ) {
    this.backlog = backlog;
    this.store = store;
    this.processing = processing;
    this.batcher = batcher;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void grantAvailable() {
    if (!running) {
      return;
    }
    try {
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

      batcher.add(processedIds);

      if (store.counts().waiting() == 0) {
        batcher.flushRemaining();
      }
    } catch (Exception e) {
      log.warn("Error in GrantScheduler.polling: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }
}

