package com.example.rollingChunks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.awspring.cloud.sqs.annotation.SqsListener;

/**
 * Example listener to demonstrate "online" notifications via SNS->SQS.
 * The durable source of truth is still the DB change feed (see /api/changes).
 */
@Component
public class ChangeFeedSqsListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeFeedSqsListener.class);

    @Value("${app.change-feed.notify.enabled:true}")
    private boolean enabled;

    @SqsListener("${app.change-feed.notify.queue-name:file-change-queue}")
    public void onMessage(String body) {
        if (!enabled) return;
        // Local demo: just log the event. Real clients would use this as a prompt to query /api/changes.
        log.info("Change-feed notification received: {}", body);
    }
}
