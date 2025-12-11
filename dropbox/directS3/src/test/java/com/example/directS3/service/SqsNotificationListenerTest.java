package com.example.directS3.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.directS3.service.SqsNotificationListener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

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
            when(sqsClient.listQueues(any(ListQueuesRequest.class))).thenReturn(ListQueuesResponse.builder().queueUrls(queueUrl).build());

        String s3Key = "my%2Ffile.txt"; // url-encoded in S3 event
        String snsEnvelope = "{\"Message\": \"{\\\"Records\\\":[{\\\"s3\\\":{\\\"object\\\":{\\\"key\\\":\\\"" + s3Key + "\\\"}}}]}\"}";

        Message msg = Message.builder().body(snsEnvelope).receiptHandle("rh").build();
            when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().messages(msg).build());

        listener.poll();

        verify(fileService).markAsAvailable("my/file.txt");
            verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
