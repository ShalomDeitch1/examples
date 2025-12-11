package com.example.directS3.service;

import java.util.List;

import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Thin wrapper around SQS SDK used to make testing easier (mock this in unit tests).
 */
public interface SqsClientWrapper {
    List<String> listQueues(ListQueuesRequest request);

    ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request);

    void deleteMessage(DeleteMessageRequest request);
}
