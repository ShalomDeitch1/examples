package com.example.chunkS3.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    private final long binaryChunkSizeBytes;

    public ConfigController(@Value("${app.chunk.binary.size-bytes}") long binaryChunkSizeBytes) {
        this.binaryChunkSizeBytes = binaryChunkSizeBytes;
    }

    public record ChunkingConfigResponse(long binaryChunkSizeBytes) {
    }

    @GetMapping("/api/config/chunking")
    public ChunkingConfigResponse chunking() {
        return new ChunkingConfigResponse(binaryChunkSizeBytes);
    }
}
