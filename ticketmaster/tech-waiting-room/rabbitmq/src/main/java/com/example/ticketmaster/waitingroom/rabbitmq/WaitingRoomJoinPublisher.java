package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomJoinPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final RabbitWaitingRoomProperties properties;

  public WaitingRoomJoinPublisher(RabbitTemplate rabbitTemplate, RabbitWaitingRoomProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publishJoin(String sessionId) {
    rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), sessionId);
  }
}
