package com.example.rollingChunks.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.sqs.annotation.SqsListener;

@Component
public class SqsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationListener.class);

    private final ChunkedFileService chunkedFileService;
    private final ObjectMapper objectMapper;

    public SqsNotificationListener(ChunkedFileService chunkedFileService, ObjectMapper objectMapper) {
        this.chunkedFileService = chunkedFileService;
        this.objectMapper = objectMapper;
        log.info("SqsNotificationListener initialized - using @SqsListener");
    }

    @SqsListener("s3-notif-queue-${aws.s3.bucket}")
    public void onMessage(String body) {
        try {
            handleMessage(body);
        } catch (RuntimeException e) {
            // rethrow so the container can handle retries/DLQ
            throw e;
        } catch (Exception e) {
            log.error("Failed to process SQS message", e);
            throw new RuntimeException(e);
        }
    }

    // queue lookup & receive/delete are managed by awspring when using @SqsListener

    private void handleMessage(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);

        // SQS may contain SNS envelope.
        if (root.has("Type") && root.has("Message")) {
            String message = root.get("Message").asText();
            root = objectMapper.readTree(message);
        }

        if (!root.has("Records")) return;

        for (JsonNode rec : root.get("Records")) {
            JsonNode s3 = rec.get("s3");
            if (s3 == null) continue;
            JsonNode obj = s3.get("object");
            if (obj == null) continue;
            JsonNode keyNode = obj.get("key");
            if (keyNode == null) continue;

            String key = urlDecodeKey(keyNode.asText());
            chunkedFileService.onObjectCreated(key);
        }
    }

    private static String urlDecodeKey(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return key;
        }
    }
}
