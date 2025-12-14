package com.example.chunkS3.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class FileVersion implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID fileId;

    @Enumerated(EnumType.STRING)
    private ChunkingStrategy chunkingStrategy;

    /** True only for TEXT_LINES_NORMALIZED_LF. */
    private boolean textNewlinesNormalized;

    /** Only meaningful for TEXT_LINES_NORMALIZED_LF. */
    private boolean endsWithNewline;

    /** Size in bytes after client-side reassembly (may differ from stored chunk bytes for text). */
    private long reassembledSizeBytes;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "file_version_parts", joinColumns = @JoinColumn(name = "version_id"))
    @OrderColumn(name = "part_order")
    private List<ChunkPart> parts = new ArrayList<>();

    @Transient
    private boolean isNew = true;

    protected FileVersion() {}

    public static FileVersion create(UUID fileId,
                                     ChunkingStrategy chunkingStrategy,
                                     boolean textNewlinesNormalized,
                                     boolean endsWithNewline,
                                     long reassembledSizeBytes,
                                     List<ChunkPart> parts,
                                     FileStatus initialStatus) {
        FileVersion v = new FileVersion();
        v.id = UUID.randomUUID();
        v.fileId = fileId;
        v.chunkingStrategy = chunkingStrategy;
        v.textNewlinesNormalized = textNewlinesNormalized;
        v.endsWithNewline = endsWithNewline;
        v.reassembledSizeBytes = reassembledSizeBytes;
        v.parts = new ArrayList<>(parts);
        v.status = initialStatus;
        v.createdAt = Instant.now();
        return v;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public boolean isTextNewlinesNormalized() {
        return textNewlinesNormalized;
    }

    public boolean isEndsWithNewline() {
        return endsWithNewline;
    }

    public long getReassembledSizeBytes() {
        return reassembledSizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public List<ChunkPart> getParts() {
        return parts;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }
}
