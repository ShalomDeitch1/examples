package com.example.rollingChunks.model;

/**
 * How the client chunked the content.
 *
 * Stage 3 (educational):
 * - TEXT_LINES_NORMALIZED_LF: text is normalized (CRLF -> LF) and split by lines.
 *   This is easy to observe but is not the "standard" Dropbox-style CDC algorithm.
 *
 * Stage 4:
 * - ROLLING_TEXT_NORMALIZED_LF: text is normalized (CRLF -> LF) and split using a rolling/content-defined chunker
 *   so inserts don't shift all subsequent chunk boundaries.
 *
 * Other:
 * - FIXED_256_KIB: binary/other content is split into fixed 256 KiB chunks (last chunk may be smaller).
 */
public enum ChunkingStrategy {
    TEXT_LINES_NORMALIZED_LF,
    ROLLING_TEXT_NORMALIZED_LF,
    FIXED_256_KIB
}
