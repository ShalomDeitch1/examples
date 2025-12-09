package com.example.shorturl;

import com.example.shorturl.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CachePerformanceTest {

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

    @Test
    void testCacheImprovement() {
        // 1. Create Short URL
        Map<String, String> request = Map.of("url", "https://performance.com");
        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/shorturl", request, Map.class);
        String fullUrl = (String) createResponse.getBody().get("shortUrl");
        String shortId = extractShortId(fullUrl);

        // 2. Configure Delay (e.g., 500ms) to make DB slow
        restTemplate.postForEntity("/shorturl/config/delay?min=500&max=500", null, Void.class);

        // 3. First Read (Cache Miss -> DB Hit + Delay)
        long start1 = System.currentTimeMillis();
        restTemplate.getForEntity("/shorturl/" + shortId, Void.class);
        long duration1 = System.currentTimeMillis() - start1;
        System.out.println("First Read (Cache Miss): " + duration1 + "ms");

        assertThat(duration1).isGreaterThanOrEqualTo(500);

        // 4. Second Read (Cache Hit -> Fast)
        long start2 = System.currentTimeMillis();
        restTemplate.getForEntity("/shorturl/" + shortId, Void.class);
        long duration2 = System.currentTimeMillis() - start2;
        System.out.println("Second Read (Cache Hit): " + duration2 + "ms");

        // Should be VERY fast (e.g., < 50ms), definitely much faster than delay
        assertThat(duration2).isLessThan(100);
        assertThat(duration2).isLessThan(duration1);
    }

    private String extractShortId(String fullUrl) {
        // Extract the last segment of the URL path
        return fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
    }
}
