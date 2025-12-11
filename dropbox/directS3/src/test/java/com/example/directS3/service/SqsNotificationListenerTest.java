package com.example.directS3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
class SqsNotificationListenerTest {

    @Mock
    SqsClient sqsClient;

    @Mock
    FileService fileService;

    ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    SqsNotificationListener listener;

    @Test
    void poll_parsesSnsEnvelopeAndMarksAvailable() {
        String queueUrl = "http://local/s3-notif-queue-1";
        // ensure the listener will look for the expected queue name suffix
        try {
            var f = SqsNotificationListener.class.getDeclaredField("bucketName");
            f.setAccessible(true);
            f.set(listener, "1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(sqsClient.listQueues(any(ListQueuesRequest.class))).thenReturn(ListQueuesResponse.builder().queueUrls(queueUrl).build());

        String s3Key = "my%2Ffile.txt"; // url-encoded in S3 event
        try {
            var event = mapper.writeValueAsString(
                java.util.Map.of("Records", java.util.List.of(
                    java.util.Map.of("s3", java.util.Map.of(
                        "object", java.util.Map.of("key", s3Key)
                    ))
                )));
            var envelope = mapper.writeValueAsString(java.util.Map.of("Message", event));
            Message msg = Message.builder().body(envelope).receiptHandle("rh").build();
            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().messages(msg).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        listener.poll();

        verify(fileService).markAsAvailable("my/file.txt");
            verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
