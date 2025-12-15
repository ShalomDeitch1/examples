package com.example.rollingChunks.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    @Value("${app.change-feed.notify.enabled:true}")
    private boolean changeFeedNotifyEnabled;

    @Value("${app.change-feed.notify.topic-name:file-change-topic}")
    private String changeFeedTopicName;

    @Value("${app.change-feed.notify.queue-name:file-change-queue}")
    private String changeFeedQueueName;

    public MessagingInitializer(SnsClient snsClient, SqsClient sqsClient, S3Client s3Client) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initMessaging() {
        try {
            // Ensure the S3 bucket exists before creating notifications or subscribing resources.
            ensureBucketExists();

            // Ensure bucket has permissive CORS for local browser-based PUTs (development only).
            ensureBucketCors();

            String topicArn = createTopicForBucket();
            String queueUrl = createQueueForBucket();
            String queueArn = fetchQueueArn(queueUrl);
            attachPolicyAllowingSns(topicArn, queueArn, queueUrl);
            snsClient.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn).build());
            log.info("Subscribed queue {} to topic {}", queueArn, topicArn);

            configureBucketNotifications(topicArn);
            log.info("Configured S3 bucket notifications to SNS topic");

            if (changeFeedNotifyEnabled) {
                initChangeFeedNotifications();
            }
        } catch (Exception e) {
            log.error("Failed to initialize messaging resources", e);
        }
    }

    void initChangeFeedNotifications() {
        String topicArn = snsClient.createTopic(CreateTopicRequest.builder().name(changeFeedTopicName).build()).topicArn();
        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(changeFeedQueueName).build()).queueUrl();
        String queueArn = fetchQueueArn(queueUrl);
        attachPolicyAllowingSns(topicArn, queueArn, queueUrl);
        snsClient.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn).build());
        log.info("Change feed SNS->SQS ready. topicArn={}, queueName={}", topicArn, changeFeedQueueName);
    }

    String createTopicForBucket() {
        String topicName = "s3-notif-topic-" + bucketName;
        CreateTopicResponse topicResp = snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build());
        String topicArn = topicResp.topicArn();
        log.info("Created/Found SNS topic: {}", topicArn);
        return topicArn;
    }

    String createQueueForBucket() {
        String queueName = "s3-notif-queue-" + bucketName;
        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
        log.info("Created/Found SQS queue: {}", queueUrl);
        return queueUrl;
    }

    String fetchQueueArn(String queueUrl) {
        return sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(software.amazon.awssdk.services.sqs.model.QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes()
                .get(software.amazon.awssdk.services.sqs.model.QueueAttributeName.QUEUE_ARN);
    }

    void attachPolicyAllowingSns(String topicArn, String queueArn, String queueUrl) {
        String policy = String.format("""
                {
                  \"Version\":\"2012-10-17\",
                  \"Statement\":[
                    {
                      \"Effect\":\"Allow\",
                      \"Principal\":{\"AWS\":\"*\"},
                      \"Action\":\"sqs:SendMessage\",
                      \"Resource\":\"%s\",
                      \"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"%s\"}}
                    }
                  ]
                }
                """, queueArn, topicArn);

        Map<software.amazon.awssdk.services.sqs.model.QueueAttributeName, String> attrs = new HashMap<>();
        attrs.put(software.amazon.awssdk.services.sqs.model.QueueAttributeName.POLICY, policy);
        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attrs).build());
    }

    void ensureBucketExists() {
        try {
            // Try head first to avoid spurious errors when bucket already exists.
            s3Client.headBucket(b -> b.bucket(bucketName));
            log.info("Bucket {} already exists", bucketName);
            return;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
            // fall through to create
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // If 404/NotFound treat as missing, otherwise log and continue to attempt create
            if (e.statusCode() != 404) {
                log.warn("HeadBucket failed for {}: {}", bucketName, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Unexpected error while checking bucket {}: {}", bucketName, e.getMessage());
        }

        try {
            s3Client.createBucket(b -> b.bucket(bucketName));
            log.info("Created bucket {}", bucketName);
        } catch (Exception e) {
            log.warn("Failed to create bucket {}: {}", bucketName, e.getMessage());
        }
    }

    void configureBucketNotifications(String topicArn) {
        TopicConfiguration topicConfig = TopicConfiguration.builder()
                .topicArn(topicArn)
                .events(Event.S3_OBJECT_CREATED_PUT)
                .build();

        NotificationConfiguration notif = NotificationConfiguration.builder()
                .topicConfigurations(topicConfig)
                .build();

        s3Client.putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .notificationConfiguration(notif)
                .build());
    }

    void ensureBucketCors() {
        try {
            // Use a simple HTTP PUT to the LocalStack S3 endpoint to set a permissive CORS XML.
            // This avoids relying on SDK model types that may vary by version.
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<CORSConfiguration>\n" +
                    "  <CORSRule>\n" +
                    "    <AllowedOrigin>*</AllowedOrigin>\n" +
                    "    <AllowedMethod>GET</AllowedMethod>\n" +
                    "    <AllowedMethod>PUT</AllowedMethod>\n" +
                    "    <AllowedMethod>POST</AllowedMethod>\n" +
                    "    <AllowedMethod>HEAD</AllowedMethod>\n" +
                    "    <AllowedMethod>DELETE</AllowedMethod>\n" +
                    "    <AllowedHeader>*</AllowedHeader>\n" +
                    "    <ExposeHeader>ETag</ExposeHeader>\n" +
                    "    <MaxAgeSeconds>3000</MaxAgeSeconds>\n" +
                    "  </CORSRule>\n" +
                    "</CORSConfiguration>";

            URI putUri = URI.create(s3Endpoint + "/" + bucketName + "?cors");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(putUri)
                    .PUT(HttpRequest.BodyPublishers.ofString(xml))
                    .header("Content-Type", "application/xml")
                    .build();

            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            log.info("Set bucket CORS on {} => status {}", bucketName, resp.statusCode());
        } catch (Exception e) {
            log.warn("Failed to set bucket CORS for {}: {}", bucketName, e.getMessage());
        }
    }
}
