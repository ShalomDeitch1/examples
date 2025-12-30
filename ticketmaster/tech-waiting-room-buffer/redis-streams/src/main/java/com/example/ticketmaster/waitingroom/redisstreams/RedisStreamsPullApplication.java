/**
 * Why this exists in this repo:
 * - Standalone Spring Boot entry point to run the Redis Streams pull-mode buffer example.
 *
 * Real system notes:
 * - Production setups use managed Redis, consumer groups, pending-entry handling, and robust observability.
 *
 * How it fits this example flow:
 * - Boots the HTTP API + scheduled poller that reads from a Redis stream on each tick.
 */
package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.ticketmaster.waitingroom")
public class RedisStreamsPullApplication {
  public static void main(String[] args) {
    SpringApplication.run(RedisStreamsPullApplication.class, args);
  }
}
