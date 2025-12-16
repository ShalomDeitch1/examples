package com.example.rollingChunks.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    private final long binaryChunkSizeBytes;
    private final int rollingMinChunkBytes;
    private final int rollingAvgChunkBytes;
    private final int rollingMaxChunkBytes;

    public ConfigController(
            @Value("${app.chunk.binary.size-bytes}") long binaryChunkSizeBytes,
            @Value("${app.rolling.min-chunk-bytes}") int rollingMinChunkBytes,
            @Value("${app.rolling.avg-chunk-bytes}") int rollingAvgChunkBytes,
            @Value("${app.rolling.max-chunk-bytes}") int rollingMaxChunkBytes
    ) {
        this.binaryChunkSizeBytes = binaryChunkSizeBytes;
        this.rollingMinChunkBytes = rollingMinChunkBytes;
        this.rollingAvgChunkBytes = rollingAvgChunkBytes;
        this.rollingMaxChunkBytes = rollingMaxChunkBytes;
    }

    public record ChunkingConfigResponse(
            long binaryChunkSizeBytes,
            int rollingMinChunkBytes,
            int rollingAvgChunkBytes,
            int rollingMaxChunkBytes
    ) {
    }

    @GetMapping("/api/config/chunking")
    public ChunkingConfigResponse chunking() {
        return new ChunkingConfigResponse(
                binaryChunkSizeBytes,
                rollingMinChunkBytes,
                rollingAvgChunkBytes,
                rollingMaxChunkBytes
        );
    }
}
