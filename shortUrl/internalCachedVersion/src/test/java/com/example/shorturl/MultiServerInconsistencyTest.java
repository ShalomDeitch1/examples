package com.example.shorturl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the consistency problem with internal caches in a multi-server
 * environment.
 * 
 * Note: This test manually starts two Spring Boot application contexts,
 * so it does NOT use @SpringBootTest annotation.
 */
public class MultiServerInconsistencyTest {

    @Test
    void testStaleCacheOnDelete() throws Exception {
        // We will start two separate Spring Boot applications (Servers A and B)
        // They share the SAME H2 file database (configured in application.properties)
        // because they run in the same process but different contexts with random
        // ports.

        ConfigurableApplicationContext serverA = startApp(0);
        ConfigurableApplicationContext serverB = startApp(0);

        try {
            int portA = getPort(serverA);
            int portB = getPort(serverB);

            RestTemplate client = new RestTemplate();
            // Configure RestTemplate to NOT throw exceptions on 4xx/5xx status codes
            // We want to inspect the status codes in our assertions
            client.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
                @Override
                protected boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
                    return false; // Never treat any status as an error
                }
            });

            String itemUrl = "https://consistency-problem.com";

            // 1. Create Short URL on Server A
            System.out.println("Creating URL on Server A...");
            ResponseEntity<Map> createResp = client.postForEntity(
                    "http://localhost:" + portA + "/shorturl",
                    Map.of("url", itemUrl),
                    Map.class);
            String shortId = (String) createResp.getBody().get("shortUrl");
            assertThat(shortId).isNotNull();

            // 2. Read on Server A (Populates Cache A)
            System.out.println("Reading from Server A (Populating Cache A)...");
            client.getForEntity("http://localhost:" + portA + "/shorturl/" + shortId, Void.class);

            // 3. Read on Server B (Populates Cache B)
            System.out.println("Reading from Server B (Populating Cache B)...");
            ResponseEntity<Void> respB1 = client.getForEntity("http://localhost:" + portB + "/shorturl/" + shortId,
                    Void.class);
            assertThat(respB1.getStatusCode()).isEqualTo(HttpStatus.FOUND);

            // 4. DELETE on Server A (Removes from DB, Evicts Cache A)
            // Cache B stays untouched!
            System.out.println("Deleting on Server A...");
            client.delete("http://localhost:" + portA + "/shorturl/" + shortId);

            // Verify A knows it's gone
            ResponseEntity<Void> respA2 = client.getForEntity("http://localhost:" + portA + "/shorturl/" + shortId,
                    Void.class);
            assertThat(respA2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            // 5. THE PROBLEM: Read on Server B -> Returns FOUND because of stale cache
            System.out.println("Reading from Server B (Expecting Stale Cache Hit)...");
            ResponseEntity<Void> respB2 = client.getForEntity("http://localhost:" + portB + "/shorturl/" + shortId,
                    Void.class);
            assertThat(respB2.getStatusCode()).isEqualTo(HttpStatus.FOUND); // Should be 404 if consistent, but it's
                                                                            // 302!
            System.out.println("Server B returned STALE data as expected.");

            // 6. Wait for TTL (2.1 seconds)
            System.out.println("Waiting for TTL expiration...");
            Thread.sleep(2200);

            // 7. Read on Server B -> Now 404 (Cache Expired -> DB Check -> Not Found)
            System.out.println("Reading from Server B (After TTL)...");
            ResponseEntity<Void> respB3 = client.getForEntity("http://localhost:" + portB + "/shorturl/" + shortId,
                    Void.class);
            assertThat(respB3.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            System.out.println("Server B eventually became consistent.");

        } finally {
            serverA.close();
            serverB.close();
        }
    }

    private ConfigurableApplicationContext startApp(int port) {
        return new SpringApplicationBuilder(ShortUrlApplication.class)
                .properties("server.port=" + port)
                .properties("spring.jmx.enabled=false") // Avoid JMX conflicts
                .run();
    }

    private int getPort(ConfigurableApplicationContext context) {
        return context.getEnvironment().getProperty("local.server.port", Integer.class);
    }
}
