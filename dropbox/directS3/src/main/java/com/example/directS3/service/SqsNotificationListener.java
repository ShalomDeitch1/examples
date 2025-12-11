package com.example.directS3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@EnableScheduling
public class SqsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationListener.class);

    private final SqsClient sqsClient;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket}")
    private String bucketName;

    // In this simple implementation we assume the queue URL is discovered/known;
    // For brevity we'll use a fixed prefix search. In a production system store the queue URL.
    private String queueUrl = null;

    public SqsNotificationListener(SqsClient sqsClient, FileService fileService, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.fileService = fileService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        log.info("SqsNotificationListener initialized - polling will start shortly");
    }

    @Scheduled(fixedDelay = 2000)
    public void poll() {
        try {
            if (queueUrl == null) {
                // find queue with the fixed name matching the bucket
                String expectedQueueName = "s3-notif-queue-" + bucketName;
                var urls = sqsClient.listQueues(ListQueuesRequest.builder().build()).queueUrls();
                log.info("Found {} queues in total, looking for queue containing: {}", urls.size(), expectedQueueName);
                for (String url : urls) {
                    log.debug("Checking queue URL: {}", url);
                    if (url.contains(expectedQueueName)) {
                        queueUrl = url;
                        break;
                    }
                }
                if (queueUrl == null) {
                    log.warn("No queue matching '{}' pattern found", expectedQueueName);
                    return;
                }
                log.info("Using SQS queue: {}", queueUrl);
            }

            ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(5).waitTimeSeconds(5).build();
            var resp = sqsClient.receiveMessage(req);
            log.debug("Received {} messages from SQS", resp.messages().size());
            for (Message m : resp.messages()) {
                try {
                    String body = m.body();
                    // SQS may contain SNS envelope; detect and extract
                    JsonNode root = objectMapper.readTree(body);
                    JsonNode s3Event = root.has("Message") ? objectMapper.readTree(root.get("Message").asText()) : root;

                    if (s3Event.has("Records")) {
                        for (JsonNode record : s3Event.get("Records")) {
                            if (record.has("s3") && record.get("s3").has("object")) {
                                String key = record.get("s3").get("object").get("key").asText();
                                String decoded = URLDecoder.decode(key, StandardCharsets.UTF_8);
                                log.info("Processing S3 notification for key: {}", decoded);
                                try {
                                    fileService.markAsAvailable(decoded);
                                } catch (RuntimeException e) {
                                    log.error("Failed to mark file available", e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process SQS message", e);
                } finally {
                    // delete message
                    sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
                }
            }
        } catch (Exception e) {
            log.error("Error polling SQS", e);
        }
    }
}

