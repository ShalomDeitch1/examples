package com.example.directS3.service;

import java.util.List;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Component
public class SqsClientWrapperImpl implements SqsClientWrapper {

    private final SqsClient delegate;

    public SqsClientWrapperImpl(SqsClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<String> listQueues(ListQueuesRequest request) {
        return delegate.listQueues(request).queueUrls();
    }

    @Override
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request) {
        return delegate.receiveMessage(request);
    }

    @Override
    public void deleteMessage(DeleteMessageRequest request) {
        delegate.deleteMessage(request);
    }
}
