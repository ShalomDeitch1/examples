package com.example.ticketmaster.waitingroom.sqs;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class SqsGrantPoller {
  private final SqsClient sqs;
  private final QueueUrlProvider queueUrlProvider;
  private final WaitingRoomStore store;
  private final WaitingRoomCapacityProperties capacity;

  public SqsGrantPoller(SqsClient sqs, QueueUrlProvider queueUrlProvider, WaitingRoomStore store, WaitingRoomCapacityProperties capacity) {
    this.sqs = sqs;
    this.queueUrlProvider = queueUrlProvider;
    this.store = store;
    this.capacity = capacity;
  }

  @Scheduled(fixedDelayString = "${waitingroom.poll.rate-ms:200}")
  public void pollAndGrant() {
    if (store.activeCount() >= capacity.maxActive()) {
      return;
    }

    String queueUrl = queueUrlProvider.getQueueUrl();
    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .maxNumberOfMessages(1)
        .waitTimeSeconds(0)
        .build();

    List<Message> messages = sqs.receiveMessage(request).messages();
    if (messages == null || messages.isEmpty()) {
      return;
    }

    Message message = messages.getFirst();
    String sessionId = message.body();

    boolean activated;
    try {
      activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
    } catch (IllegalArgumentException e) {
      // Unknown session; treat as stale.
      activated = true;
    }

    // Delete if granted (or stale/duplicate). If capacity is full, message will reappear later.
    if (activated) {
      sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }
  }
}
