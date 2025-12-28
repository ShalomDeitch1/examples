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
    registry.add("waitingroom.poll.rate-ms", () -> "50");
    registry.add("waitingroom.poll.initial-delay-ms", () -> "500");
    registry.add("waitingroom.capacity.max-active", () -> "10");
    registry.add("waitingroom.grant.group-size", () -> "10");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void grantsInBatchesOfTenUntilAllProcessed() {
    var client = new WaitingRoomTestClient(rest, port);
    List<String> sessionIds = client.joinMany("E1", 100);

    var progress = client.awaitAllGrantedAndReleaseCapacity(sessionIds, Duration.ofSeconds(60));
    List<WaitingRoomTestClient.GrantBatchDto> batches = progress.batches();

    assertThat(batches.size()).isGreaterThanOrEqualTo(2);
    assertThat(batches).allSatisfy(b -> assertThat(b.sessionIds().size()).isLessThanOrEqualTo(10));

    Set<String> allGranted = batches.stream().flatMap(b -> b.sessionIds().stream()).collect(Collectors.toSet());
    assertThat(allGranted).hasSize(100);
    assertThat(allGranted).containsAll(sessionIds);
    assertThat(progress.grantedSessionIds()).containsAll(sessionIds);
  }
}

