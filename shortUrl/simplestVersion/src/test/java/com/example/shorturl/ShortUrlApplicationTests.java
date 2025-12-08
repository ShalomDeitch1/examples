package com.example.shorturl;

import com.example.shorturl.service.UrlService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShortUrlApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlService urlService;

    private static String createdShortUrl;

    @Test
    @Order(1)
    void createShortUrl() {
        Map<String, String> request = Map.of("url", "https://google.com");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/shorten", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("shortUrl");

        createdShortUrl = (String) response.getBody().get("shortUrl");
        System.out.println("Created Short URL: " + createdShortUrl);
    }

    @Test
    @Order(2)
    void resolveShortUrl() {
        assertThat(createdShortUrl).isNotNull();
        ResponseEntity<Void> response = restTemplate.getForEntity("/" + createdShortUrl, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://google.com");
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void verifyDataLossAfterRestart() {
        // @DirtiesContext BEFORE_METHOD forces Spring to reload the context before this
        // test runs.
        // This simulates a restart. The static field 'createdShortUrl' persists across
        // tests,
        // but the UrlService inside the NEW context should be empty.

        assertThat(createdShortUrl).isNotNull();

        ResponseEntity<Void> response = restTemplate.getForEntity("/" + createdShortUrl, Void.class);

        // Step 1 Expectation: Data is LOST, so we should get 404.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
