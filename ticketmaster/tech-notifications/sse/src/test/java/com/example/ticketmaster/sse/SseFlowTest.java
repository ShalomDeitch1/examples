package com.example.ticketmaster.sse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SseFlowTest {

    @LocalServerPort
    private int port;

    @Test
    void streamsNotReadyWaitingReadyOverSingleConnection() throws Exception {
        String userId = "flow-user";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        URI subscribeUri = URI.create("http://localhost:%d/subscribe/%s".formatted(port, userId));
        HttpRequest subscribeRequest = HttpRequest.newBuilder(subscribeUri)
            // IMPORTANT: Don't set HttpRequest timeout for SSE.
            // JDK HttpClient request timeout covers the *whole* response lifecycle.
            // SSE is a streaming response that stays open until the server completes it.
            // We enforce our own time limit using the latch below.
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        // Written by the reader thread; asserted by the test thread.
        List<String> statuses = new CopyOnWriteArrayList<>();
        CountDownLatch readyLatch = new CountDownLatch(1);

        // IMPORTANT: subscribe() may not commit headers until the first event is sent.
        // If we call client.send() here on the test thread, we can deadlock:
        // - test thread waits for subscribe response headers
        // - server waits to send first event until /start is called
        // - but /start isn't called until after send() returns
        // So: run the subscribe request + stream reader on a background thread.
        CompletableFuture<Void> readerFuture = CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<java.io.InputStream> subscribeResponse = client.send(
                        subscribeRequest,
                        HttpResponse.BodyHandlers.ofInputStream()
                );
                if (subscribeResponse.statusCode() != 200) {
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(subscribeResponse.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) {
                            continue;
                        }

                        String payload = line.substring("data:".length()).trim();
                        String status = extractJsonField(payload, "status");
                        if (status == null) {
                            continue;
                        }

                        statuses.add(status);
                        if ("READY".equals(status)) {
                            readyLatch.countDown();
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
                // If the server completes the emitter, the stream closes and we exit.
            }
        });

        URI startUri = URI.create("http://localhost:%d/start/%s".formatted(port, userId));
        HttpRequest startRequest = HttpRequest.newBuilder(startUri)
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> startResponse = client.send(startRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(startResponse.statusCode()).isEqualTo(200);

        boolean reachedReady = readyLatch.await(8, TimeUnit.SECONDS);
        assertThat(reachedReady).isTrue();

        // Ensure the background reader has had a chance to drain up to READY.
        // (No need to wait long; READY implies the reader hit its return path.)
        readerFuture.get(1, TimeUnit.SECONDS);

        assertThat(statuses).containsSubsequence("NOT_READY", "WAITING", "READY");
    }

    private static String extractJsonField(String json, String fieldName) {
        // Minimal parser to avoid extra test dependencies.
        // Example payload: {"userId":"u1","status":"WAITING","message":"...","timestamp":"..."}
        String needle = "\"" + fieldName + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) {
            return null;
        }
        start += needle.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return json.substring(start, end);
    }
}
