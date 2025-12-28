package com.example.ticketmaster.waitingroom.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketmaster.waitingroom.testsupport.WaitingRoomTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
    registry.add("waitingroom.processing.rate-ms", () -> "50");
    registry.add("waitingroom.processing.initial-delay-ms", () -> "1500");
    registry.add("waitingroom.processing.batch-size", () -> "10");
    registry.add("waitingroom.kafka.topic", () -> "waiting-room-joins");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void grantsInBatchesOfTenUntilAllProcessed() {
    var client = new WaitingRoomTestClient(rest, port);
    List<String> requestIds = client.joinManyFast("E1", 100, 20);

    assertThat(requestIds).allSatisfy(id -> assertThat(id)
        .withFailMessage("SESSION ID NOT NUMERIC: %s", id)
        .matches("\\d+"));
    WaitingRoomTestClient.assertNumericConsecutiveIds(requestIds);

    var progress = client.awaitAllProcessed(requestIds, Duration.ofSeconds(60));
    List<WaitingRoomTestClient.ProcessingBatchDto> batches = progress.batches();

      assertThat(batches.size()).withFailMessage("Expected at least 10 batches, was %s", batches.size()).isGreaterThanOrEqualTo(10);
      assertThat(batches).allSatisfy(b -> assertThat(b.requestIds().size()).isLessThanOrEqualTo(10));

    Set<String> allGranted = batches.stream().flatMap(b -> b.requestIds().stream()).collect(Collectors.toSet());
    assertThat(allGranted).hasSize(100);
    assertThat(allGranted).containsAll(requestIds);
    assertThat(progress.processedRequestIds()).containsAll(requestIds);

      WaitingRoomTestClient.assertAndLogGrouping(requestIds, batches);
  }
}

