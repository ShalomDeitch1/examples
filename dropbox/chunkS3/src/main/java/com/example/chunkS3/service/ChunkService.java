package com.example.chunkS3.service;

import org.springframework.stereotype.Service;

/**
 * Deprecated Stage 3 API.
 *
 * Stage 3 is implemented as client-side chunking + direct-to-S3 uploads via presigned URLs.
 * The server only manages metadata and notifications.
 */
@Service
@Deprecated
public class ChunkService {

    public void unsupported() {
        throw new UnsupportedOperationException("ChunkService is removed in Stage 3. Use ChunkedFileService.");
    }
}
