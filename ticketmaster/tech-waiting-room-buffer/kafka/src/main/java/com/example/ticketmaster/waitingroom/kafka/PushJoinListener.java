/**
 * Why this exists in this repo:
 * - Receives Kafka messages (push delivery) and buffers request IDs for tick-based processing.
 *
 * Real system notes:
 * - Many systems process immediately in the listener or hand off to a durable internal queue; an in-memory handoff can lose data on crash.
 *
 * How it fits this example flow:
 * - Kafka → {@code @KafkaListener} → backlog buffer; later the scheduled processor drains the backlog.
 */
package com.example.ticketmaster.waitingroom.kafka;

import com.example.ticketmaster.waitingroom.core.push.GroupCollector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PushJoinListener {
  private final GroupCollector groupCollector;

  public PushJoinListener(GroupCollector groupCollector) {
    this.groupCollector = groupCollector;
  }

  @KafkaListener(topics = "${waitingroom.kafka.topic}")
  public void onRequest(String requestId) {
    groupCollector.enqueue(requestId);
  }
}
