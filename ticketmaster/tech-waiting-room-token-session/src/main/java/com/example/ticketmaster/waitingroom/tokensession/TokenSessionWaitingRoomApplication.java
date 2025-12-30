package com.example.ticketmaster.waitingroom.tokensession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TokenSessionWaitingRoomApplication {
  public static void main(String[] args) {
    SpringApplication.run(TokenSessionWaitingRoomApplication.class, args);
  }
}
