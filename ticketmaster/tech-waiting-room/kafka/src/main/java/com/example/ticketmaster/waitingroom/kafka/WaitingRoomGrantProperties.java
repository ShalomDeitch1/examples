package com.example.ticketmaster.waitingroom.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.grant")
public record WaitingRoomGrantProperties(int groupSize) {
  public WaitingRoomGrantProperties {
    if (groupSize <= 0) {
      throw new IllegalArgumentException("groupSize must be > 0");
    }
  }
}
