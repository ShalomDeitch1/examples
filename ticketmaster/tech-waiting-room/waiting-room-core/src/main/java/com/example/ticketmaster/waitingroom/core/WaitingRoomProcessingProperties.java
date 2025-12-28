package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.processing")
public record WaitingRoomProcessingProperties(int batchSize) {
  public WaitingRoomProcessingProperties {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be > 0");
    }
  }
}
