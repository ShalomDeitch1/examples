package com.example.directS3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import static org.mockito.Mockito.*;

class MessagingInitializerTest {

    private SnsClient snsClient;
    private SqsClient sqsClient;
    private S3Client s3Client;
    private MessagingInitializer initializer;

    @BeforeEach
    void setUp() {
        snsClient = mock(SnsClient.class);
        sqsClient = mock(SqsClient.class);
        s3Client = mock(S3Client.class);
        initializer = new MessagingInitializer(snsClient, sqsClient, s3Client);
        // set a known bucket name via reflection
        try {
            var f = MessagingInitializer.class.getDeclaredField("bucketName");
            f.setAccessible(true);
            f.set(initializer, "unittest-bucket");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initMessaging_createsTopicQueueAndConfiguresNotifications() {
        // Use simple stubbing for createTopic/createQueue/getQueueAttributes
        when(snsClient.createTopic(any(software.amazon.awssdk.services.sns.model.CreateTopicRequest.class))).thenReturn(software.amazon.awssdk.services.sns.model.CreateTopicResponse.builder().topicArn("arn:aws:sns:us-east-1:000000000000:s3-notif-topic-unittest-bucket").build());
        when(sqsClient.createQueue(any(software.amazon.awssdk.services.sqs.model.CreateQueueRequest.class))).thenReturn(software.amazon.awssdk.services.sqs.model.CreateQueueResponse.builder().queueUrl("http://localhost/queue").build());
        when(sqsClient.getQueueAttributes(any(software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest.class))).thenReturn(software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse.builder().attributes(java.util.Map.of(software.amazon.awssdk.services.sqs.model.QueueAttributeName.QUEUE_ARN, "arn:aws:sqs:us-east-1:000000000000:queue")).build());

        initializer.initMessaging();

        verify(snsClient, atLeastOnce()).createTopic((CreateTopicRequest) any());
        verify(sqsClient, atLeastOnce()).createQueue((CreateQueueRequest) any());
        verify(sqsClient, atLeastOnce()).setQueueAttributes((SetQueueAttributesRequest) any());
        verify(snsClient, atLeastOnce()).subscribe((SubscribeRequest) any());
        verify(s3Client, atLeastOnce()).putBucketNotificationConfiguration((PutBucketNotificationConfigurationRequest) any());
    }
}
