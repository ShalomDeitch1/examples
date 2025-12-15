package com.example.rollingChunks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.rollingChunks.model.ChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class ChangeFeedNotifier {

    private static final Logger log = LoggerFactory.getLogger(ChangeFeedNotifier.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.change-feed.notify.enabled:true}")
    private boolean enabled;

    @Value("${app.change-feed.notify.topic-name:file-change-topic}")
    private String topicName;

    private volatile String topicArn;

    public ChangeFeedNotifier(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void publishBestEffort(ChangeEvent event) {
        if (!enabled) return;
        try {
            String arn = ensureTopicArn();
            String message = objectMapper.writeValueAsString(new ChangeEventMessage(
                    event.getId(),
                    event.getEventType().name(),
                    event.getFileId(),
                    event.getVersionId(),
                    event.getFileName(),
                    event.getContentType(),
                    event.getSizeBytes(),
                    event.getCreatedAt()
            ));
            snsClient.publish(PublishRequest.builder().topicArn(arn).message(message).build());
        } catch (Exception e) {
            log.warn("Failed to publish change-feed SNS notification: {}", e.getMessage());
        }
    }

    private String ensureTopicArn() {
        String current = topicArn;
        if (current != null) return current;
        synchronized (this) {
            if (topicArn == null) {
                topicArn = snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
                log.info("Created/Found change-feed SNS topic: {}", topicArn);
            }
            return topicArn;
        }
    }

    public record ChangeEventMessage(
            Long eventId,
            String eventType,
            java.util.UUID fileId,
            java.util.UUID versionId,
            String fileName,
            String contentType,
            long sizeBytes,
            java.time.Instant createdAt
    ) {}
}
