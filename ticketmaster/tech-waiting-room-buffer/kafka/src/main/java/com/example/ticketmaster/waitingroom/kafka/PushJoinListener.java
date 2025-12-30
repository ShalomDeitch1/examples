package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.push.JoinBacklog;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PushJoinListener {
  private final JoinBacklog backlog;

  public PushJoinListener(JoinBacklog backlog) {
    this.backlog = backlog;
  }

  @KafkaListener(topics = "${waitingroom.kafka.topic}")
  public void onRequest(String requestId) {
    backlog.enqueue(requestId);
  }
}
