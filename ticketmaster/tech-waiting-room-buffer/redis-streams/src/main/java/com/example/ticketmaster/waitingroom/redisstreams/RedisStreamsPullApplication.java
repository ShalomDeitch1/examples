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
