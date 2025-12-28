package com.example.ticketmaster.waitingroom.redisstreams;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamInitializer implements InitializingBean {
  private final RedisConnectionFactory connectionFactory;
  private final RedisStreamsWaitingRoomProperties properties;

  public RedisStreamInitializer(RedisConnectionFactory connectionFactory, RedisStreamsWaitingRoomProperties properties) {
    this.connectionFactory = connectionFactory;
    this.properties = properties;
  }

  @Override
  public void afterPropertiesSet() {
    try (RedisConnection connection = connectionFactory.getConnection()) {
      byte[] stream = properties.stream().getBytes(StandardCharsets.UTF_8);
      byte[] group = properties.consumerGroup().getBytes(StandardCharsets.UTF_8);

      // XGROUP CREATE <stream> <group> $ MKSTREAM
      Object result = connection.execute(
          "XGROUP",
          "CREATE".getBytes(StandardCharsets.UTF_8),
          stream,
          group,
          "$".getBytes(StandardCharsets.UTF_8),
          "MKSTREAM".getBytes(StandardCharsets.UTF_8)
      );
    } catch (Exception e) {
      // BUSYGROUP means it already exists
      if (e.getMessage() == null || !e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
  }
}
