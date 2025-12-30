/**
 * Why this exists in this repo:
 * - Publishes join messages into RabbitMQ (the pipe) for the pull-mode demo.
 *
 * Real system notes:
 * - Production publishing typically uses publisher confirms, retries, DLQs, and schema/versioning for payloads.
 *
 * How it fits this example flow:
 * - Called by the controller to publish the request ID to the exchange.
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PullJoinPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final RabbitPullProperties properties;

  public PullJoinPublisher(RabbitTemplate rabbitTemplate, RabbitPullProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publishRequest(String requestId) {
    rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), requestId);
  }
}
