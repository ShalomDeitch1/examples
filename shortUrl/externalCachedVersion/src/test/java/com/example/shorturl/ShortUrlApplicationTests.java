package com.example.shorturl;

import com.example.shorturl.service.UrlService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class ShortUrlApplicationTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlService urlService;

    @LocalServerPort
    private int port;

    // Static variable to share state between test methods (simulating external
    // client knowledge)
    private static String createdShortUrl;

    @Test
    @Order(1)
    void createShortUrl() {
        Map<String, String> request = Map.of("url", "https://google.com");
        ResponseEntity<Map> response = restTemplate.postForEntity("/shorturl", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("shortUrl");

        createdShortUrl = (String) response.getBody().get("shortUrl");
        System.out.println("Created Short URL: " + createdShortUrl);
    }

    @Test
    @Order(2)
    void resolveShortUrl() {
        assertThat(createdShortUrl).isNotNull();
        ResponseEntity<Void> response = restTemplate.getForEntity("/shorturl/" + createdShortUrl, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://google.com");
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void verifyDataPersistenceAfterRestart() {
        // @DirtiesContext BEFORE_METHOD forces Spring to reload the context before this
        // test runs.
        // This simulates a restart. The static field 'createdShortUrl' persists,
        // and because we use a file-based H2 DB, the data should ALSO persist.

        assertThat(createdShortUrl).isNotNull();

        // After context reload, we need to use absolute URL
        String url = "http://localhost:" + port + "/shorturl/" + createdShortUrl;
        ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);

        // Expectation: Data is FOUND (Persisted), so we should get 302 Found.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://google.com");
    }
}
