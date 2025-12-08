package com.example.shorturl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates that using an external cache (Redis) SOLVES the consistency
 * problem
 * in a multi-server environment.
 */
@Testcontainers
public class MultiServerConsistencyTest {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    @Test
    void testConsistentCacheOnDelete() throws Exception {
        // Start two separate Spring Boot applications (Servers A and B)
        // configured to use the SAME Redis container and SAME H2 DB.

        String redisHost = redis.getHost();
        Integer redisPort = redis.getFirstMappedPort();

        System.out.println("Redis running at " + redisHost + ":" + redisPort);

        ConfigurableApplicationContext serverA = startApp(0, redisHost, redisPort);
        ConfigurableApplicationContext serverB = startApp(0, redisHost, redisPort);

        try {
            int portA = getPort(serverA);
            int portB = getPort(serverB);

            RestTemplate client = new RestTemplate();
            // Configure Error Handler to allow inspecting 404 responses
            client.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
                @Override
                protected boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
                    return false;
                }
            });

            String itemUrl = "https://consistency-solved.com";

            // 1. Create Short URL on Server A
            System.out.println("Creating URL on Server A...");
            ResponseEntity<Map> createResp = client.postForEntity(
                    "http://localhost:" + portA + "/shorturl",
                    Map.of("url", itemUrl),
                    Map.class);
            String fullUrl = (String) createResp.getBody().get("shortUrl");
            String shortId = extractShortId(fullUrl);

            // 2. Read on Server A (Populates Redis)
            System.out.println("Reading from Server A (Populating Redis)...");
            client.getForEntity("http://localhost:" + portA + "/shorturl/" + shortId, Void.class);

            // 3. Read on Server B (Cache Hit from Redis)
            System.out.println("Reading from Server B (Checking Redis)...");
            ResponseEntity<Void> respB1 = client.getForEntity("http://localhost:" + portB + "/shorturl/" + shortId,
                    Void.class);
            assertThat(respB1.getStatusCode()).isEqualTo(HttpStatus.FOUND);

            // 4. DELETE on Server A (Removes from DB, Evicts from Redis)
            System.out.println("Deleting on Server A...");
            client.delete("http://localhost:" + portA + "/shorturl/" + shortId);

            // 5. Read on Server B -> Should be 404 IMMEDIATELY
            // because Redis is the single source of truth for cache.
            System.out.println("Reading from Server B (Expecting Consistent 404)...");
            ResponseEntity<Void> respB2 = client.getForEntity("http://localhost:" + portB + "/shorturl/" + shortId,
                    Void.class);

            // Assertion: It should be NOT_FOUND.
            // In internal cache version, this was FOUND (Stale). Now it should be correct.
            assertThat(respB2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            System.out.println("Server B returned 404 as expected! Consistency achieved.");

        } finally {
            serverA.close();
            serverB.close();
        }
    }

    private ConfigurableApplicationContext startApp(int port, String redisHost, int redisPort) {
        return new SpringApplicationBuilder(ShortUrlApplication.class)
                .properties("server.port=" + port)
                .properties("spring.jmx.enabled=false")
                .properties("spring.data.redis.host=" + redisHost)
                .properties("spring.data.redis.port=" + redisPort)
                .run();
    }

    private int getPort(ConfigurableApplicationContext context) {
        return context.getEnvironment().getProperty("local.server.port", Integer.class);
    }

    private String extractShortId(String fullUrl) {
        // Extract the last segment of the URL path
        return fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
    }
}
