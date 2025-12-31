/**
 * Why this exists in this repo:
 * - Spring Boot entrypoint for the RabbitMQ pull-mode waiting-room buffer demo.
 *
 * Real system notes:
 * - Real deployments separate API concerns, durable state, and broker configuration; observability is typically metrics/tracing.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + scheduled poller that drains RabbitMQ in fixed-size batches.
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class RabbitMqPullApplication {
  public static void main(String[] args) {
    SpringApplication.run(RabbitMqPullApplication.class, args);
  }
}
