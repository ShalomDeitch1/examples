package test.java.com.example.ticketmaster.waitingroom.sqs;

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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.ticketmaster.waitingroom.sqs.WaitingRoomSession;
import com.example.ticketmaster.waitingroom.sqs.WaitingRoomSessionStatus;
import com.example.ticketmaster.waitingroom.sqs.WaitingRoomSqsApplication;

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
