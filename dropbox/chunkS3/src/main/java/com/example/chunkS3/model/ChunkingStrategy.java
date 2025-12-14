package com.example.chunkS3.model;

/**
 * How the client chunked the content.
 *
 * - TEXT_LINES_NORMALIZED_LF: text content is normalized (CRLF -> LF) on the client and split into lines.
 * - FIXED_64_BYTES: binary/other content is split into fixed 64-byte chunks (last chunk may be smaller).
 */
public enum ChunkingStrategy {
    TEXT_LINES_NORMALIZED_LF,
    FIXED_64_BYTES
}
