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
