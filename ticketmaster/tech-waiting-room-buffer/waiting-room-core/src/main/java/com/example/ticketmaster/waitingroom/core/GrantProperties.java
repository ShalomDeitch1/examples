package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.grant")
public record GrantProperties(int groupSize) {
  public GrantProperties {
    if (groupSize <= 0) {
      throw new IllegalArgumentException("groupSize must be > 0");
    }
  }
}
