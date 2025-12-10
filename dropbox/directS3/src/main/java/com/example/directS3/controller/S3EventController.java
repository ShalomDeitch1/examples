package com.example.directS3.controller;

import com.example.directS3.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/hooks/s3")
public class S3EventController {

    private static final Logger log = LoggerFactory.getLogger(S3EventController.class);
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public S3EventController(FileService fileService, ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody String body) {
        log.info("Received S3 Event: {}", body);

        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("Records")) {
                for (JsonNode record : root.get("Records")) {
                    if (record.has("s3") && record.get("s3").has("object")) {
                        String key = record.get("s3").get("object").get("key").asText();
                        // Keys coming from S3 events might be URL encoded
                        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
                        log.info("Processing ObjectCreated for key: {}", decodedKey);
                        try {
                            fileService.markAsAvailable(decodedKey);
                        } catch (RuntimeException e) {
                            log.error("Failed to mark file as available for key: " + decodedKey, e);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse S3 event", e);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
