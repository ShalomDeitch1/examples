package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomJoinListener {
  private final JoinBacklog backlog;

  public WaitingRoomJoinListener(JoinBacklog backlog) {
    this.backlog = backlog;
  }

  @KafkaListener(topics = "${waitingroom.kafka.topic}")
  public void onJoin(String sessionId) {
    backlog.enqueue(sessionId);
  }
}
