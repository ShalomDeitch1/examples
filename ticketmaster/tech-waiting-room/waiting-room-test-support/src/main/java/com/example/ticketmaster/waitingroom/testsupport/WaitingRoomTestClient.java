package com.example.ticketmaster.waitingroom.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class WaitingRoomTestClient {
  private final TestRestTemplate rest;
  private final int port;

  public WaitingRoomTestClient(TestRestTemplate rest, int port) {
    this.rest = rest;
    this.port = port;
  }

  public record GrantBatchDto(long batchNumber, String grantedAt, List<String> sessionIds) {
  }

  public record GrantProgress(Set<String> grantedSessionIds, List<GrantBatchDto> batches) {
  }

  public List<String> joinMany(String eventId, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> join(eventId, "U" + i))
        .toList();
  }

  public String join(String eventId, String userId) {
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

  public void leave(String sessionId) {
    rest.postForEntity("http://localhost:" + port + "/api/waiting-room/sessions/" + sessionId + ":leave", null, Void.class);
  }

  public List<GrantBatchDto> getBatches() {
    String url = "http://localhost:" + port + "/api/waiting-room/grant-batches";
    GrantBatchDto[] response = rest.getForObject(url, GrantBatchDto[].class);
    if (response == null) {
      return List.of();
    }
    return Arrays.asList(response);
  }

  public GrantProgress awaitAllGrantedAndReleaseCapacity(List<String> expectedSessionIds, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    Set<Long> releasedBatches = new HashSet<>();
    Set<String> granted = new HashSet<>();

    while (Instant.now().isBefore(deadline)) {
      List<GrantBatchDto> batches = getBatches();
      for (GrantBatchDto batch : batches) {
        if (!releasedBatches.add(batch.batchNumber())) {
          continue;
        }
        for (String sessionId : batch.sessionIds()) {
          granted.add(sessionId);
          leave(sessionId);
        }
      }

      if (granted.containsAll(expectedSessionIds)) {
        return new GrantProgress(Set.copyOf(granted), getBatches());
      }

      sleep(Duration.ofMillis(50));
    }

    return new GrantProgress(Set.copyOf(granted), getBatches());
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
