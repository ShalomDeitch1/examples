package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WaitingRoomKafkaApplication {
  public static void main(String[] args) {
    SpringApplication.run(WaitingRoomKafkaApplication.class, args);
  }
}
