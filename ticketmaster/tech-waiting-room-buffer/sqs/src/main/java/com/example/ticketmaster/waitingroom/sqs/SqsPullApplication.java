/**
 * Why this exists in this repo:
 * - Standalone Spring Boot entry point to run the SQS pull-mode buffer example.
 *
 * Real system notes:
 * - Production setups use IAM, retries/backoff, metrics, and careful visibility timeout tuning.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + scheduled poller that pulls from SQS on each tick.
 */
package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class SqsPullApplication {
  public static void main(String[] args) {
    SpringApplication.run(SqsPullApplication.class, args);
  }
}
