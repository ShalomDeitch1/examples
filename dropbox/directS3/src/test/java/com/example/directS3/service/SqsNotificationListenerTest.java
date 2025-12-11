package com.example.directS3.service;

import java.util.List;
import java.util.Map;

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
                Map.of("Records", List.of(
                    Map.of("s3", Map.of(
                        "object", Map.of("key", s3Key)
                    ))
                )));
            var envelope = mapper.writeValueAsString(Map.of("Message", event));
            Message msg = Message.builder().body(envelope).receiptHandle("rh").build();
            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().messages(msg).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        listener.poll();

        verify(fileService).markAsAvailable("my/file.txt");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void poll_setsQueueUrlWhenNull() throws Exception {
        // set bucketName
        var bucketField = SqsNotificationListener.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(listener, "test");

        when(sqsClient.listQueues(any(ListQueuesRequest.class))).thenReturn(ListQueuesResponse.builder().queueUrls("http://local/s3-notif-queue-test").build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().build());

        listener.poll();

        assert listener.queueUrl.equals("http://local/s3-notif-queue-test");
    }

    @Test
    void processMessage_marksFileAvailable() throws Exception {
        String event = mapper.writeValueAsString(
            Map.of("Records", List.of(
                Map.of("s3", Map.of("object", Map.of("key", "file.txt")))
            ))
        );
        Message msg = Message.builder().body(event).build();

        listener.processMessage(msg);

        verify(fileService).markAsAvailable("file.txt");
    }

    @Test
    void processMessage_handlesSnsEnvelope() throws Exception {
        String s3Event = mapper.writeValueAsString(
            Map.of("Records", List.of(
                Map.of("s3", Map.of("object", Map.of("key", "encoded%2Ffile.txt")))
            ))
        );
        String envelope = mapper.writeValueAsString(Map.of("Message", s3Event));
        Message msg = Message.builder().body(envelope).build();

        listener.processMessage(msg);

        verify(fileService).markAsAvailable("encoded/file.txt");
    }
}
