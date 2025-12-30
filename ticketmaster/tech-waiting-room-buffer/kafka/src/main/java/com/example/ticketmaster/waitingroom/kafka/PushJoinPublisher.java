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
