package com.example.chunkS3.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ChunkPart {

    private String hash;

    /** Length (bytes) of the chunk object stored in S3. */
    private int lengthBytes;

    protected ChunkPart() {}

    public ChunkPart(String hash, int lengthBytes) {
        this.hash = hash;
        this.lengthBytes = lengthBytes;
    }

    public String getHash() {
        return hash;
    }

    public int getLengthBytes() {
        return lengthBytes;
    }
}
