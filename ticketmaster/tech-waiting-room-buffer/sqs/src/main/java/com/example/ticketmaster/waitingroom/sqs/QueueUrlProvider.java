/**
 * Why this exists in this repo:
 * - Small helper to resolve a queue URL from a configured name so the rest of the demo code stays clean.
 *
 * Real system notes:
 * - Many systems inject the queue URL/ARN directly (from IaC outputs) and avoid resolving it at runtime.
 *
 * How it fits this example flow:
 * - Used by the publisher and poller to target the correct SQS queue.
 */
package com.example.ticketmaster.waitingroom.sqs;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@Component
public class QueueUrlProvider {
  private final SqsClient sqs;
  private final SqsProperties properties;
  private final AtomicReference<String> queueUrl = new AtomicReference<>();

  public QueueUrlProvider(SqsClient sqs, SqsProperties properties) {
    this.sqs = sqs;
    this.properties = properties;
  }

  public String getQueueUrl() {
    String cached = queueUrl.get();
    if (cached != null) {
      return cached;
    }

    String resolved = resolveQueueUrl();
    queueUrl.compareAndSet(null, resolved);
    return queueUrl.get();
  }

  private String resolveQueueUrl() {
    try {
      return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(properties.queueName()).build()).queueUrl();
    } catch (QueueDoesNotExistException e) {
      if (!properties.autoCreateQueue()) {
        throw e;
      }
      sqs.createQueue(CreateQueueRequest.builder().queueName(properties.queueName()).build());
      return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(properties.queueName()).build()).queueUrl();
    }
  }
}
