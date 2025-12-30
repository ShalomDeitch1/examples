package com.example.ticketmaster.waitingroom.testsupport;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class TestClient {
  private final TestRestTemplate rest;
  private final int port;

  public TestClient(TestRestTemplate rest, int port) {
    this.rest = rest;
    this.port = port;
  }

  public record ProcessingBatchDto(long batchNumber, String processedAt, List<String> requestIds) {
  }

  public record CountsDto(long waiting, long processed, long total) {
  }

  public record ObservabilityDto(CountsDto counts, List<ProcessingBatchDto> batches) {
  }

  public record ProcessingProgress(Set<String> processedRequestIds, List<ProcessingBatchDto> batches) {
  }

  public static void assertAndLogGrouping(List<String> expectedRequestIds, List<ProcessingBatchDto> batches) {
    System.out.println("\n=== GROUP MEMBERSHIP (BATCHES) ===");
    for (ProcessingBatchDto batch : batches) {
      String members = String.join(",", batch.requestIds());
      System.out.println("GROUP " + batch.batchNumber() + " (size=" + batch.requestIds().size() + "): " + members);
    }

    Set<String> expected = new TreeSet<>(expectedRequestIds);
    Set<String> observed = new HashSet<>();
    Set<String> duplicates = new TreeSet<>();

    for (ProcessingBatchDto batch : batches) {
      for (String requestId : batch.requestIds()) {
        if (!observed.add(requestId)) {
          duplicates.add(requestId);
        }
      }
    }

    Set<String> missing = new TreeSet<>(expected);
    missing.removeAll(observed);

    Set<String> extra = new TreeSet<>(observed);
    extra.removeAll(expected);

    if (!missing.isEmpty()) {
      System.out.println("MISSING ITEMS (NOT IN ANY GROUP): " + missing);
    }
    if (!duplicates.isEmpty()) {
      System.out.println("DUPLICATE ITEMS (IN MULTIPLE GROUPS): " + duplicates);
    }
    if (!extra.isEmpty()) {
      System.out.println("EXTRA ITEMS (NOT EXPECTED): " + extra);
    }

    assertThat(missing)
        .withFailMessage("MISSING ITEMS (NOT IN ANY GROUP): %s", missing)
        .isEmpty();
    assertThat(duplicates)
        .withFailMessage("DUPLICATE ITEMS (IN MULTIPLE GROUPS): %s", duplicates)
        .isEmpty();
    assertThat(extra)
        .withFailMessage("EXTRA ITEMS (NOT EXPECTED): %s", extra)
        .isEmpty();

    String summary = batches.stream()
        .map(b -> b.batchNumber() + "->" + b.requestIds().size())
        .collect(Collectors.joining(", "));
    System.out.println("GROUP SUMMARY (batch->size): " + summary);
    System.out.println("=== END GROUP MEMBERSHIP ===\n");
  }

  public static void assertNumericConsecutiveIds(List<String> requestIds) {
    List<Long> parsed = requestIds.stream().map(Long::parseLong).sorted().toList();
    if (parsed.isEmpty()) {
      return;
    }

    for (int i = 1; i < parsed.size(); i++) {
      long previous = parsed.get(i - 1);
      long current = parsed.get(i);
      if (current != previous + 1) {
        System.out.println("NON-CONSECUTIVE IDS: prev=" + previous + " current=" + current);
        break;
      }
    }

    for (int i = 1; i < parsed.size(); i++) {
      long previous = parsed.get(i - 1);
      long current = parsed.get(i);
      assertThat(current)
          .withFailMessage("NON-CONSECUTIVE IDS: prev=%s current=%s", previous, current)
          .isEqualTo(previous + 1);
    }
  }

  public List<String> joinMany(String eventId, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> enqueue(eventId, "U" + i))
        .toList();
  }

  public List<String> joinManyFast(String eventId, int count, int concurrency) {
    if (concurrency <= 0) {
      throw new IllegalArgumentException("concurrency must be > 0");
    }

    ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    try {
      List<CompletableFuture<String>> futures = IntStream.range(0, count)
          .mapToObj(i -> CompletableFuture.supplyAsync(() -> enqueue(eventId, "U" + i), executor))
          .toList();

      return futures.stream().map(CompletableFuture::join).toList();
    } finally {
      executor.shutdownNow();
    }
  }

  public String enqueue(String eventId, String userId) {
    String url = "http://localhost:" + port + "/api/waiting-room/requests";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("eventId", eventId, "userId", userId), headers);

    ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);
    assertThat(response.getStatusCode().value()).isEqualTo(202);

    Map body = response.getBody();
    assertThat(body).isNotNull();
    Object value = body.get("requestId");
    assertThat(value).isInstanceOf(String.class);
    return (String) value;
  }

  public ObservabilityDto getObservability() {
    String url = "http://localhost:" + port + "/api/waiting-room/observability";
    return rest.getForObject(url, ObservabilityDto.class);
  }

  public ProcessingProgress awaitAllProcessed(List<String> expectedRequestIds, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    Set<String> processed = new HashSet<>();

    while (Instant.now().isBefore(deadline)) {
      ObservabilityDto observability = getObservability();
      if (observability != null && observability.batches() != null) {
        for (ProcessingBatchDto batch : observability.batches()) {
          for (String requestId : batch.requestIds()) {
            processed.add(requestId);
          }
        }
      }

      if (processed.containsAll(expectedRequestIds)) {
        ObservabilityDto finalObs = getObservability();
        List<ProcessingBatchDto> batches = finalObs == null || finalObs.batches() == null ? List.of() : finalObs.batches();
        return new ProcessingProgress(Set.copyOf(processed), batches);
      }

      sleep(Duration.ofMillis(50));
    }

    ObservabilityDto finalObs = getObservability();
    List<ProcessingBatchDto> batches = finalObs == null || finalObs.batches() == null ? List.of() : finalObs.batches();
    return new ProcessingProgress(Set.copyOf(processed), batches);
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
