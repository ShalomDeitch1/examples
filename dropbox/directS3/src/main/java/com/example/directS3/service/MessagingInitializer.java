package com.example.directS3.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

@Component
public class MessagingInitializer {

    private static final Logger log = LoggerFactory.getLogger(MessagingInitializer.class);

    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final S3Client s3Client;

    @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket}")
    private String bucketName;

    public MessagingInitializer(SnsClient snsClient, SqsClient sqsClient, S3Client s3Client) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initMessaging() {
        try {
            String topicName = "s3-notif-topic-" + bucketName;
            CreateTopicResponse topicResp = snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build());
            String topicArn = topicResp.topicArn();
            log.info("Created/Found SNS topic: {}", topicArn);

            String queueName = "s3-notif-queue-" + bucketName;
            String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
            log.info("Created/Found SQS queue: {}", queueUrl);

            // Get Queue ARN
                String queueArn = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl).attributeNames(software.amazon.awssdk.services.sqs.model.QueueAttributeName.QUEUE_ARN).build()).attributes().get(software.amazon.awssdk.services.sqs.model.QueueAttributeName.QUEUE_ARN);

                // Allow SNS to publish to SQS by setting queue policy
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"" + queueArn + "\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"" + topicArn + "\"}}}]}";
                Map<software.amazon.awssdk.services.sqs.model.QueueAttributeName, String> attrs = new HashMap<>();
                attrs.put(software.amazon.awssdk.services.sqs.model.QueueAttributeName.POLICY, policy);
                sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attrs).build());

            // Subscribe SQS queue to SNS topic
            snsClient.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn).build());
            log.info("Subscribed queue {} to topic {}", queueArn, topicArn);

            // Ensure the bucket exists (create if missing)
            try {
                s3Client.createBucket(b -> b.bucket(bucketName));
                log.info("Created bucket {}", bucketName);
            } catch (Exception e) {
                log.info("Bucket {} may already exist or could not be created: {}", bucketName, e.getMessage());
            }

            // Configure S3 bucket notifications to the topic (ObjectCreated:Put)
            TopicConfiguration topicConfig = TopicConfiguration.builder().topicArn(topicArn).events(Event.S3_OBJECT_CREATED_PUT).build();
            NotificationConfiguration notif = NotificationConfiguration.builder().topicConfigurations(topicConfig).build();
            s3Client.putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder()
                    .bucket(bucketName).notificationConfiguration(notif).build());

            log.info("Configured S3 bucket notifications to SNS topic");
        } catch (Exception e) {
            log.error("Failed to initialize messaging resources", e);
        }
    }
}
