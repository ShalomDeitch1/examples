package com.example.ticketmaster.waitingroom.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WaitingRoomRabbitMqIntegrationTest {

  @Container
  static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    registry.add("waitingroom.grant.rate-ms", () -> "50");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void joinEventuallyBecomesActive() {
    String sessionId = join("E1", "U1");
    WaitingRoomSession session = awaitStatus(sessionId, WaitingRoomSessionStatus.ACTIVE, Duration.ofSeconds(10));
    assertThat(session.status()).isEqualTo(WaitingRoomSessionStatus.ACTIVE);
  }

  private String join(String eventId, String userId) {
    String url = "http://localhost:" + port + "/api/waiting-room/sessions";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("eventId", eventId, "userId", userId), headers);

    ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);
    assertThat(response.getStatusCode().value()).isEqualTo(202);

    Object value = response.getBody().get("sessionId");
    assertThat(value).isInstanceOf(String.class);
    return (String) value;
  }

  private WaitingRoomSession awaitStatus(String sessionId, WaitingRoomSessionStatus expected, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    WaitingRoomSession last = null;

    while (Instant.now().isBefore(deadline)) {
      last = rest.getForObject("http://localhost:" + port + "/api/waiting-room/sessions/" + sessionId, WaitingRoomSession.class);
      if (last != null && last.status() == expected) {
        return last;
      }
      sleep(Duration.ofMillis(50));
    }

    return last;
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
