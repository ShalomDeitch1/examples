package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WaitingRoomRabbitMqApplication {
  public static void main(String[] args) {
    SpringApplication.run(WaitingRoomRabbitMqApplication.class, args);
  }
}
