/**
 * Why this exists in this repo:
 * - Publishes join requests into Kafka (the pipe) for the pull-mode demo.
 *
 * Real system notes:
 * - In production you typically publish asynchronously with retries/backoff, schemas, and monitoring.
 * - You also choose explicit semantics for the HTTP endpoint:
 *   - either only acknowledge the HTTP request after the broker has ACKed the message, or
 *   - accept quickly but persist the intent first (e.g., DB outbox) and publish in the background.
 *
 * Why this demo blocks briefly:
 * - Tests spin up Kafka via Testcontainers, and there can be a short window where the broker/topic leader is not ready.
 * - If we "fire-and-forget" publishes during that window, the API can return 202 + requestId even though the request
 *   never reached Kafka, so it will never be processed.
 * - To keep the example deterministic, we wait up to 2 seconds for the broker ACK and fail fast if publishing fails.
 *
 * How it fits this example flow:
 * - Called by the controller to publish (eventId key, requestId value) to Kafka.
 */
package com.example.ticketmaster.waitingroom.kafka.pull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PullJoinPublisher {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final KafkaPullProperties properties;

  public PullJoinPublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaPullProperties properties) {
    this.kafkaTemplate = kafkaTemplate;
    this.properties = properties;
  }

  public void publishRequest(String eventId, String requestId) {
    try {
      kafkaTemplate.send(properties.topic(), eventId, requestId).get(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing join request to Kafka", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException("Failed to publish join request to Kafka", e);
    }
  }
}
