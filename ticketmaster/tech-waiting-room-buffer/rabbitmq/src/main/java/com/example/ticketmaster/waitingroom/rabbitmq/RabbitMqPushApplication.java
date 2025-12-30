/**
 * Why this exists in this repo:
 * - Standalone Spring Boot entry point to run the RabbitMQ push-mode buffer example.
 *
 * Real system notes:
 * - Production deployments include structured logging, metrics, health probes, and environment-specific broker config.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + Rabbit listener + scheduled tick processor for the RabbitMQ module.
 */
package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class RabbitMqPushApplication {
  public static void main(String[] args) {
    SpringApplication.run(RabbitMqPushApplication.class, args);
  }
}
