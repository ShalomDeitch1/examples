package com.example.ticketmaster.waitingroom.core.push;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-memory backlog used only for "push" style queue integrations (Kafka/RabbitMQ listeners).
 *
 * <p>Why this exists:
 * <ul>
 *   <li>Kafka/RabbitMQ integrations in this repo are listener-driven: the broker pushes messages into the app.</li>
 *   <li>The waiting-room "buffer" approach is tick-driven: a scheduled job processes up to N items per tick.</li>
 * </ul>
 * This class bridges those models by buffering request IDs in-process so the scheduled batch processor can
 * repeatedly {@link #poll()} up to N IDs each tick.
 *
 * <p>How it is used:
 * <ul>
 *   <li>Listener callback (push) calls {@link #enqueue(String)}.</li>
 *   <li>Scheduled processor (tick) calls {@link #poll()} in a loop.</li>
 * </ul>
 *
 * <p>Notes:
 * <ul>
 *   <li>This is not durable. If the process dies, any buffered IDs are lost (the real queue provides durability).</li>
 *   <li>"Pull" style integrations (SQS polling, Redis Streams reads) do not need this because the scheduler
 *       pulls directly from the broker.</li>
 * </ul>
 */
public class JoinBacklog {
  private final BlockingQueue<String> requestIds = new LinkedBlockingQueue<>();

  public void enqueue(String requestId) {
    requestIds.add(requestId);
  }

  public Optional<String> poll() {
    return Optional.ofNullable(requestIds.poll());
  }
}
