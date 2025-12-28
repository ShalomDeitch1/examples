package com.example.ticketmaster.waitingroom.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WaitingRoomKafkaIntegrationTest {

  @Container
  static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("waitingroom.grant.rate-ms", () -> "50");
    registry.add("waitingroom.grant.initial-delay-ms", () -> "500");
    registry.add("waitingroom.capacity.max-active", () -> "10");
    registry.add("waitingroom.grant.group-size", () -> "10");
    registry.add("waitingroom.kafka.topic", () -> "waiting-room-joins");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void grantsInBatchesOfTenUntilAllProcessed() {
    List<String> sessionIds = IntStream.range(0, 100)
        .mapToObj(i -> join("E1", "U" + i))
        .toList();

    Set<String> granted = awaitAllGrantedAndReleaseCapacity(sessionIds, Instant.now().plus(Duration.ofSeconds(60)));
    List<GrantBatch> batches = getBatches();

    assertThat(batches.size()).isGreaterThanOrEqualTo(2);
    assertThat(batches).allSatisfy(b -> assertThat(b.sessionIds().size()).isLessThanOrEqualTo(10));

    Set<String> allGranted = batches.stream().flatMap(b -> b.sessionIds().stream()).collect(Collectors.toSet());
    assertThat(allGranted).hasSize(100);
    assertThat(allGranted).containsAll(sessionIds);
    assertThat(granted).containsAll(sessionIds);
  }

  private record GrantBatch(long groupId, String grantedAt, List<String> sessionIds) {
  }

  private String join(String eventId, String userId) {
    String url = "http://localhost:" + port + "/api/waiting-room/sessions";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("eventId", eventId, "userId", userId), headers);

    ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);
    assertThat(response.getStatusCode().value()).isEqualTo(202);

    Map body = response.getBody();
    assertThat(body).isNotNull();
    Object value = body.get("sessionId");
    assertThat(value).isInstanceOf(String.class);
    return (String) value;
  }

  private List<GrantBatch> getBatches() {
    String url = "http://localhost:" + port + "/api/waiting-room/grant-batches";
    GrantBatch[] response = rest.getForObject(url, GrantBatch[].class);
    if (response == null) {
      return List.of();
    }
    return Arrays.asList(response);
  }

  private Set<String> awaitAllGrantedAndReleaseCapacity(List<String> expectedSessionIds, Instant deadline) {
    Set<String> expected = Set.copyOf(expectedSessionIds);
    Set<String> alreadyReleased = new HashSet<>();
    Set<String> granted = new HashSet<>();

    while (Instant.now().isBefore(deadline)) {
      List<GrantBatch> batches = getBatches();
      for (GrantBatch batch : batches) {
        for (String sessionId : batch.sessionIds()) {
          granted.add(sessionId);
        }
      }

      for (String sessionId : granted) {
        if (alreadyReleased.add(sessionId)) {
          leave(sessionId);
        }
      }

      if (granted.containsAll(expected)) {
        return granted;
      }

      sleep(Duration.ofMillis(50));
    }

    return granted;
  }

  private void leave(String sessionId) {
    rest.postForEntity("http://localhost:" + port + "/api/waiting-room/sessions/" + sessionId + ":leave", null, Void.class);
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

