package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PushJoinPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final RabbitProperties properties;

  public PushJoinPublisher(RabbitTemplate rabbitTemplate, RabbitProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publishRequest(String requestId) {
    rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), requestId);
  }
}
