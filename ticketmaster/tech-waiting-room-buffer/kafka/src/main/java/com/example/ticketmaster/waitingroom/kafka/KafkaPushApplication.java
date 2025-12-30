/**
 * Why this exists in this repo:
 * - Standalone Spring Boot entry point to run the Kafka “push-mode” buffer example.
 *
 * Real system notes:
 * - You’d usually run this as one service among many, with externalized config, health checks, and metrics.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + Kafka listener + scheduled tick processor for the Kafka module.
 */
package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class KafkaPushApplication {
  public static void main(String[] args) {
    SpringApplication.run(KafkaPushApplication.class, args);
  }
}
