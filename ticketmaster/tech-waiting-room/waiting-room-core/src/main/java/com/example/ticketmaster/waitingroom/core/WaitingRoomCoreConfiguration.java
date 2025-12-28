package com.example.ticketmaster.waitingroom.core;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WaitingRoomCapacityProperties.class, WaitingRoomGrantProperties.class})
public class WaitingRoomCoreConfiguration {

  @Bean
  public WaitingRoomStore waitingRoomStore() {
    return new WaitingRoomStore();
  }

  @Bean
  public GrantHistory grantHistory() {
    return new GrantHistory();
  }
}
