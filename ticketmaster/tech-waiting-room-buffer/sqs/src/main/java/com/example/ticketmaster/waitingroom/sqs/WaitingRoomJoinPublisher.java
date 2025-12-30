package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class WaitingRoomJoinPublisher {
  private final SqsClient sqs;
  private final QueueUrlProvider queueUrlProvider;

  public WaitingRoomJoinPublisher(SqsClient sqs, QueueUrlProvider queueUrlProvider) {
    this.sqs = sqs;
    this.queueUrlProvider = queueUrlProvider;
  }

  public void publishRequest(String requestId) {
    sqs.sendMessage(SendMessageRequest.builder()
        .queueUrl(queueUrlProvider.getQueueUrl())
        .messageBody(requestId)
        .build());
  }
}
