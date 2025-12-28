package com.example.ticketmaster.waitingroom.kafka;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

@Component
public class JoinBacklog {
  private final BlockingQueue<String> sessionIds = new LinkedBlockingQueue<>();

  public void enqueue(String sessionId) {
    sessionIds.add(sessionId);
  }

  public Optional<String> poll() {
    return Optional.ofNullable(sessionIds.poll());
  }
}
