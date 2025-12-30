/**
 * Why this exists in this repo:
 * - Holds SQS-specific configuration for the demo (queue name/region/etc.).
 *
 * Real system notes:
 * - Production uses per-environment queues and IAM; queue policies and DLQs are managed as infra.
 *
 * How it fits this example flow:
 * - Used by the queue URL provider and SQS client wiring.
 */
package com.example.ticketmaster.waitingroom.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waitingroom.sqs")
public record SqsProperties(
    String queueName,
    String region,
    String accessKey,
    String secretKey,
    String endpoint,
    boolean autoCreateQueue
) {
}
