package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomJoinPublisher {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final KafkaWaitingRoomProperties properties;

  public WaitingRoomJoinPublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaWaitingRoomProperties properties) {
    this.kafkaTemplate = kafkaTemplate;
    this.properties = properties;
  }

  public void publishJoin(String eventId, String sessionId) {
    kafkaTemplate.send(properties.topic(), eventId, sessionId);
  }
}
