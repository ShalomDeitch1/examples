package com.example.chunkS3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class SqsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationListener.class);

    private final SqsClient sqsClient;
    private final ChunkedFileService chunkedFileService;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private String queueUrl;

    public SqsNotificationListener(SqsClient sqsClient, ChunkedFileService chunkedFileService, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.chunkedFileService = chunkedFileService;
        this.objectMapper = objectMapper;
        log.info("SqsNotificationListener initialized - polling will start shortly");
    }

    @Scheduled(fixedDelay = 2000)
    public void poll() {
        try {
            ensureQueueUrl();
            if (queueUrl == null) return;

            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(1)
                    .build();

            var resp = sqsClient.receiveMessage(req);
            for (Message m : resp.messages()) {
                try {
                    handleMessage(m.body());
                    sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
                } catch (Exception e) {
                    log.error("Failed to process SQS message", e);
                }
            }
        } catch (Exception e) {
            log.error("Error polling SQS", e);
        }
    }

    private void ensureQueueUrl() {
        if (queueUrl != null) return;

        String expectedName = "s3-notif-queue-" + bucketName;
        List<String> urls = sqsClient.listQueues(ListQueuesRequest.builder().build()).queueUrls();
        for (String url : urls) {
            if (url.endsWith("/" + expectedName) || url.contains(expectedName)) {
                queueUrl = url;
                log.info("Using SQS queue: {}", queueUrl);
                return;
            }
        }
    }

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
