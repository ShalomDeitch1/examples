package com.example.ticketmaster.waitingroom.sqs;

import com.example.ticketmaster.waitingroom.core.GrantHistory;
import com.example.ticketmaster.waitingroom.core.WaitingRoomCapacityProperties;
import com.example.ticketmaster.waitingroom.core.WaitingRoomGrantProperties;
import com.example.ticketmaster.waitingroom.core.WaitingRoomStore;
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
  private final GrantHistory grantHistory;
  private final WaitingRoomGrantProperties grant;

  public SqsGrantPoller(
      SqsClient sqs,
      QueueUrlProvider queueUrlProvider,
      WaitingRoomStore store,
      WaitingRoomCapacityProperties capacity,
      GrantHistory grantHistory,
      WaitingRoomGrantProperties grant
  ) {
    this.sqs = sqs;
    this.queueUrlProvider = queueUrlProvider;
    this.store = store;
    this.capacity = capacity;
    this.grantHistory = grantHistory;
    this.grant = grant;
  }

  @Scheduled(
      fixedDelayString = "${waitingroom.poll.rate-ms:200}",
      initialDelayString = "${waitingroom.poll.initial-delay-ms:0}"
  )
  public void pollAndGrant() {
    int availableSlots = capacity.maxActive() - store.activeCount();
    if (availableSlots <= 0) {
      return;
    }

    int toGrant = Math.min(Math.min(availableSlots, grant.groupSize()), 10);

    String queueUrl = queueUrlProvider.getQueueUrl();
    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .maxNumberOfMessages(toGrant)
        .waitTimeSeconds(0)
        .build();

    List<Message> messages = sqs.receiveMessage(request).messages();
    if (messages == null || messages.isEmpty()) {
      return;
    }

    var activatedIds = new java.util.ArrayList<String>(toGrant);
    for (Message message : messages) {
      String sessionId = message.body();

      boolean activated;
      try {
        activated = store.tryActivateIfCapacityAllows(sessionId, capacity.maxActive());
      } catch (IllegalArgumentException e) {
        // Unknown session; treat as stale.
        activated = true;
      }

      if (activated) {
        activatedIds.add(sessionId);
        sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
        continue;
      }

      // Capacity race: keep the remaining messages for later.
      break;
    }

    grantHistory.record(activatedIds);
  }
}

