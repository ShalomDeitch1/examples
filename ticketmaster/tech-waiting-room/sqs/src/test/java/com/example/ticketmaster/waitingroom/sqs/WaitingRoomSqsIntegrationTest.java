package com.example.ticketmaster.waitingroom.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.ticketmaster.waitingroom.testsupport.WaitingRoomTestClient;
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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = WaitingRoomSqsApplication.class)
class WaitingRoomSqsIntegrationTest {

  @Container
  static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
      .withServices(LocalStackContainer.Service.SQS);

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("waitingroom.sqs.endpoint", () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
    registry.add("waitingroom.sqs.region", LOCALSTACK::getRegion);
    registry.add("waitingroom.sqs.access-key", LOCALSTACK::getAccessKey);
    registry.add("waitingroom.sqs.secret-key", LOCALSTACK::getSecretKey);
    registry.add("waitingroom.sqs.queue-name", () -> "waiting-room-joins");
    registry.add("waitingroom.sqs.auto-create-queue", () -> "true");
    registry.add("waitingroom.processing.rate-ms", () -> "50");
    registry.add("waitingroom.processing.initial-delay-ms", () -> "1500");
    registry.add("waitingroom.processing.batch-size", () -> "10");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void grantsUpToBatchSizePerTickUntilAllProcessed() {
    var client = new WaitingRoomTestClient(rest, port);
    List<String> requestIds = client.joinManyFast("E1", 100, 20);

    assertThat(requestIds).allSatisfy(id -> assertThat(id)
        .withFailMessage("SESSION ID NOT NUMERIC: %s", id)
        .matches("\\d+"));
    WaitingRoomTestClient.assertNumericConsecutiveIds(requestIds);

    var progress = client.awaitAllProcessed(requestIds, Duration.ofSeconds(60));
    List<WaitingRoomTestClient.ProcessingBatchDto> batches = progress.batches();

  int batchSize = 10;
  int expectedMinBatches = (requestIds.size() + batchSize - 1) / batchSize;

  assertThat(batches.size())
    .withFailMessage("Expected at least %s batches, was %s", expectedMinBatches, batches.size())
    .isGreaterThanOrEqualTo(expectedMinBatches);
  assertThat(batches).allSatisfy(b -> assertThat(b.requestIds().size()).isLessThanOrEqualTo(batchSize));

    Set<String> allGranted = batches.stream().flatMap(b -> b.requestIds().stream()).collect(Collectors.toSet());
    assertThat(allGranted).hasSize(100);
    assertThat(allGranted).containsAll(requestIds);
    assertThat(progress.processedRequestIds()).containsAll(requestIds);

    WaitingRoomTestClient.assertAndLogGrouping(requestIds, batches);
  }
}

