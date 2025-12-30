/**
 * Why this exists in this repo:
 * - Writes join messages into SQS (the pipe) for the pull-mode example.
 *
 * Real system notes:
 * - Production senders use retries/backoff, message attributes, and often idempotency keys or FIFO deduplication.
 *
 * How it fits this example flow:
 * - Called by the controller to send the request ID to SQS.
 */
package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class PullJoinPublisher {
  private final SqsClient sqs;
  private final QueueUrlProvider queueUrlProvider;

  public PullJoinPublisher(SqsClient sqs, QueueUrlProvider queueUrlProvider) {
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
