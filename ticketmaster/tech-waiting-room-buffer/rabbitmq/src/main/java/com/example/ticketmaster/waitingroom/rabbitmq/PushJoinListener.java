/**
 * Why this exists in this repo:
 * - Receives RabbitMQ messages (push delivery) and buffers request IDs for the scheduled processor.
 *
 * Real system notes:
 * - In-memory buffering is not durable; many systems process immediately or hand off to a durable internal work queue.
 *
 * How it fits this example flow:
 * - RabbitMQ → {@code @RabbitListener} → backlog buffer; later the scheduled processor drains the backlog.
 */
package com.example.ticketmaster.waitingroom.rabbitmq;

import com.example.ticketmaster.waitingroom.core.push.GroupCollector;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PushJoinListener {
  private final GroupCollector groupCollector;

  public PushJoinListener(GroupCollector groupCollector) {
    this.groupCollector = groupCollector;
  }

  @RabbitListener(queues = "${waitingroom.rabbitmq.queue}")
  public void onRequest(String requestId) {
    groupCollector.enqueue(requestId);
  }
}
