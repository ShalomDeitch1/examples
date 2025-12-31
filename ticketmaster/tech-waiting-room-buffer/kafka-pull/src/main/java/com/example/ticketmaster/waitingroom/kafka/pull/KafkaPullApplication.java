/**
 * Why this exists in this repo:
 * - Spring Boot entrypoint for the Kafka pull-mode waiting-room buffer demo.
 *
 * Real system notes:
 * - Real deployments run behind gateway/auth and rely on external observability; the queue and state stores are managed separately.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + scheduled poller that reads from Kafka in fixed-size batches.
 */
package com.example.ticketmaster.waitingroom.kafka.pull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class KafkaPullApplication {
  public static void main(String[] args) {
    SpringApplication.run(KafkaPullApplication.class, args);
  }
}
