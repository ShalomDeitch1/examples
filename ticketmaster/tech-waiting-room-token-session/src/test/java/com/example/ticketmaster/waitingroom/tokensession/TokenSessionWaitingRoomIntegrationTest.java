package com.example.ticketmaster.waitingroom.tokensession;

import com.example.ticketmaster.waitingroom.tokensession.api.CreateSessionRequest;
import com.example.ticketmaster.waitingroom.tokensession.api.CreateSessionResponse;
import com.example.ticketmaster.waitingroom.tokensession.model.TokenSession;
import com.example.ticketmaster.waitingroom.tokensession.model.TokenSessionStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
    classes = TokenSessionWaitingRoomApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "waitingroom.processing.capacity=10",
        "waitingroom.processing.batch-size=10",
        "waitingroom.processing.rate-ms=25",
        "waitingroom.processing.initial-delay-ms=0",
        "waitingroom.redis.stream=waiting-room-joins"
    }
)
class TokenSessionWaitingRoomIntegrationTest {
  @Container
  static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.2"));

  @DynamicPropertySource
  static void redisProps(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @LocalServerPort
  int port;

  private final TestRestTemplate rest;

  TokenSessionWaitingRoomIntegrationTest(TestRestTemplate rest) {
    this.rest = rest;
  }

  @Test
  void grantsSessionsUpToCapacityAndThenContinuesAfterLeave() {
    List<String> sessionIds = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      sessionIds.add(createSession("E1", "U" + i));
    }

    awaitActiveCount(sessionIds, 10, Duration.ofSeconds(10));
    assertMaxActive(sessionIds, 10);

    leaveFirstNActive(sessionIds, 10);

    awaitActiveCount(sessionIds, 10, Duration.ofSeconds(10));
    assertMaxActive(sessionIds, 10);

    leaveFirstNActive(sessionIds, 10);

    awaitActiveCount(sessionIds, 5, Duration.ofSeconds(10));
    assertMaxActive(sessionIds, 10);

    Set<String> left = sessionIds.stream()
      .filter(id -> getSession(id).status() == TokenSessionStatus.LEFT)
        .collect(Collectors.toSet());
    assertThat(left).hasSize(20);
  }

  private String baseUrl() {
    return "http://localhost:" + port + "/api/waiting-room";
  }

  private String createSession(String eventId, String userId) {
    ResponseEntity<CreateSessionResponse> response = rest.postForEntity(
        baseUrl() + "/sessions",
        new CreateSessionRequest(eventId, userId),
        CreateSessionResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sessionId()).isNotBlank();
    return response.getBody().sessionId();
  }

  private TokenSession getSession(String sessionId) {
    ResponseEntity<TokenSession> response = rest.getForEntity(
        baseUrl() + "/sessions/" + sessionId,
        TokenSession.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }

  private void leave(String sessionId) {
    ResponseEntity<Void> response = rest.postForEntity(
        baseUrl() + "/sessions/" + sessionId + ":leave",
        null,
        Void.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private void awaitActiveCount(List<String> sessionIds, int expectedActive, Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadline) {
      int active = countWithStatus(sessionIds, TokenSessionStatus.ACTIVE);
      if (active == expectedActive) {
        return;
      }
      sleep(50);
    }

    int active = countWithStatus(sessionIds, TokenSessionStatus.ACTIVE);
    throw new AssertionError("Timed out waiting for active=" + expectedActive + ", actual=" + active);
  }

  private void leaveFirstNActive(List<String> sessionIds, int n) {
    List<String> actives = sessionIds.stream()
        .filter(id -> getSession(id).status() == TokenSessionStatus.ACTIVE)
        .limit(n)
        .toList();

    assertThat(actives).hasSize(n);
    actives.forEach(this::leave);

    // Wait until those are visibly LEFT (avoid racing the next scheduler tick)
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (System.nanoTime() < deadline) {
      boolean allLeft = actives.stream().allMatch(id -> getSession(id).status() == TokenSessionStatus.LEFT);
      if (allLeft) {
        return;
      }
      sleep(50);
    }
  }

  private int countWithStatus(List<String> sessionIds, TokenSessionStatus status) {
    int count = 0;
    for (String id : sessionIds) {
      if (getSession(id).status() == status) {
        count++;
      }
    }
    return count;
  }

  private void assertMaxActive(List<String> sessionIds, int max) {
    int active = countWithStatus(sessionIds, TokenSessionStatus.ACTIVE);
    assertThat(active).isLessThanOrEqualTo(max);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
