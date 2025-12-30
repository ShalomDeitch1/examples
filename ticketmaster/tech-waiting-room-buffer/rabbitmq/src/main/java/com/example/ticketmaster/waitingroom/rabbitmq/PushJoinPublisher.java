/**
 * Why this exists in this repo:
 * - Publishes join messages into RabbitMQ (the pipe) for the demo.
 *
 * Real system notes:
 * - Production publishing uses confirms, retry policies, DLQs, and schema/versioning for payloads.
 *
 * How it fits this example flow:
 * - Called by the controller to publish the request ID; the listener receives it and buffers it for tick processing.
 */
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
