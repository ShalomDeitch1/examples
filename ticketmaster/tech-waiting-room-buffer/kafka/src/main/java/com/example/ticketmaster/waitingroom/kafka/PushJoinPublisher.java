/**
 * Why this exists in this repo:
 * - Writes join messages into Kafka (the pipe) for the push-mode example.
 *
 * Real system notes:
 * - You’d use strong schemas (Avro/Protobuf), keys for partitioning, retries, and idempotent producers where relevant.
 *
 * How it fits this example flow:
 * - Called by the controller when a request is created; publishes the request ID to the Kafka topic.
 */
package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PushJoinPublisher {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final KafkaProperties properties;

  public PushJoinPublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties) {
    this.kafkaTemplate = kafkaTemplate;
    this.properties = properties;
  }

  public void publishRequest(String eventId, String requestId) {
    kafkaTemplate.send(properties.topic(), eventId, requestId);
  }
}
