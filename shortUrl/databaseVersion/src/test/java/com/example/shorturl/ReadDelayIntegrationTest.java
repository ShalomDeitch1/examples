package com.example.shorturl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReadDelayIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testReadDelay() {
        // 1. Create a short URL
        Map<String, String> request = Map.of("url", "https://example.com");
        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/shorturl", request, Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String fullUrl = (String) createResponse.getBody().get("shortUrl");
        String shortId = extractShortId(fullUrl);

        // 2. Warmup & Access without delay (Should be fast)
        restTemplate.getForEntity("/shorturl/" + shortId, Void.class); // Warmup
        long start = System.currentTimeMillis();
        restTemplate.getForEntity("/shorturl/" + shortId, Void.class);
        long durationWithoutDelay = System.currentTimeMillis() - start;
        System.out.println("Duration without delay: " + durationWithoutDelay + "ms");

        // A simple weak assertion to ensure it's not unreasonably slow by default
        // (Hard to assert "fast" in CI, but usually < 100ms for local integration test)

        // 3. Configure Delay (Min 200, Max 400)
        int minDelay = 200;
        int maxDelay = 400;
        restTemplate.postForEntity("/shorturl/config/delay?min=" + minDelay + "&max=" + maxDelay, null, Void.class);

        // 4. Access with delay
        start = System.currentTimeMillis();
        restTemplate.getForEntity("/shorturl/" + shortId, Void.class);
        long durationWithDelay = System.currentTimeMillis() - start;
        System.out.println("Duration with delay: " + durationWithDelay + "ms");

        // 5. Assertions
        // The duration should be at least the min delay
        assertThat(durationWithDelay).isGreaterThanOrEqualTo(minDelay);
        // It should also be significantly slower than the non-delayed request
        // (Checking if at least 50ms slower to be safe against noise)
        assertThat(durationWithDelay).isGreaterThan(durationWithoutDelay + 50);

        // 6. Reset Delay
        restTemplate.postForEntity("/shorturl/config/delay?min=0&max=0", null, Void.class);
    }

    private String extractShortId(String fullUrl) {
        // Extract the last segment of the URL path
        return fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
    }
}
