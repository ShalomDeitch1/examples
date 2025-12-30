package com.example.ticketmaster.waitingroom.rabbitmq;

import com.example.ticketmaster.waitingroom.core.push.JoinBacklog;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PushJoinListener {
  private final JoinBacklog backlog;

  public PushJoinListener(JoinBacklog backlog) {
    this.backlog = backlog;
  }

  @RabbitListener(queues = "${waitingroom.rabbitmq.queue}")
  public void onRequest(String requestId) {
    backlog.enqueue(requestId);
  }
}
