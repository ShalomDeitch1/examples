package com.example.ticketmaster.waitingroom.sqs;

import com.example.ticketmaster.waitingroom.core.ProcessingHistory;
import com.example.ticketmaster.waitingroom.core.ProcessingProperties;
import com.example.ticketmaster.waitingroom.core.RequestStore;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class PullGrantPoller {
  private static final Logger log = LoggerFactory.getLogger(PullGrantPoller.class);
  private final SqsClient sqs;
  private final QueueUrlProvider queueUrlProvider;
  private final RequestStore store;
  private final ProcessingProperties processing;
  private final ProcessingHistory processingHistory;
  private volatile boolean running = true;

  public PullGrantPoller(
      SqsClient sqs,
      QueueUrlProvider queueUrlProvider,
      RequestStore store,
      ProcessingProperties processing,
      ProcessingHistory processingHistory
  ) {
    this.sqs = sqs;
    this.queueUrlProvider = queueUrlProvider;
    this.store = store;
    this.processing = processing;
    this.processingHistory = processingHistory;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.processing.rate-ms:200}",
      initialDelayString = "${waitingroom.processing.initial-delay-ms:0}"
  )
  public void pollAndGrant() {
    if (!running) {
      return;
    }
    try {
      int toProcess = Math.min(processing.batchSize(), 10);

      String queueUrl = queueUrlProvider.getQueueUrl();
      ReceiveMessageRequest request = ReceiveMessageRequest.builder()
          .queueUrl(queueUrl)
          .maxNumberOfMessages(toProcess)
          .waitTimeSeconds(0)
          .build();

      List<Message> messages = sqs.receiveMessage(request).messages();
      if (messages == null || messages.isEmpty()) {
        return;
      }

      var processedIds = new ArrayList<String>(toProcess);
      for (Message message : messages) {
        String requestId = message.body();
        store.markProcessed(requestId);
        processedIds.add(requestId);
        sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
      }

      processingHistory.record(processedIds);
    } catch (Exception e) {
      if (!running) {
        return;
      }
      log.debug("SQS poll failed: {}", e.toString());
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
  }
}
