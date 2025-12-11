package com.example.directS3.controller;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.directS3.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class S3EventControllerTest {

    private FileService fileService;
    private ObjectMapper mapper;
    private S3EventController controller;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        mapper = new ObjectMapper();
        controller = new S3EventController(fileService, mapper);
    }

    @Test
    void subscriptionConfirmation_dispatchesSubscribeUrl() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("Type", "SubscriptionConfirmation");
        root.put("SubscribeURL", "http://example.local/confirm");

        @SuppressWarnings("unchecked")
        Consumer<String> executor = mock(Consumer.class);
        controller.setSubscribeUrlExecutor(executor);

        controller.handleEvent(root.toString());

        verify(executor, times(1)).accept("http://example.local/confirm");
        verify(fileService, never()).markAsAvailable(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void s3Event_callsMarkAsAvailableForDecodedKey() throws Exception {
        ObjectNode s3 = mapper.createObjectNode();
        ObjectNode obj = mapper.createObjectNode();
        obj.put("key", "my%2Ffile.txt");
        s3.set("object", obj);
        ObjectNode record = mapper.createObjectNode();
        record.set("s3", s3);
        ObjectNode root = mapper.createObjectNode();
        root.set("Records", mapper.createArrayNode().add(record));

        controller.handleEvent(root.toString());

        verify(fileService, times(1)).markAsAvailable("my/file.txt");
    }
}
