package com.example.chunkS3.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

@Entity
public class FileMetadata implements Persistable<UUID> {

    @Id
    private UUID id;

    private String fileName;

    private long sizeBytes;

    private String contentType;

    /** Points to the latest AVAILABLE version id (or null if none yet). */
    private UUID currentVersionId;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long rowVersion;

    private int chunkSize;

    private int chunkCount;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @Transient
    private boolean isNew = true;

    protected FileMetadata() {}

    public static FileMetadata newPending(String fileName, String contentType) {
        FileMetadata m = new FileMetadata();
        m.id = UUID.randomUUID();
        m.fileName = fileName;
        m.contentType = contentType;
        m.sizeBytes = 0L;
        m.status = FileStatus.PENDING;
        m.createdAt = Instant.now();
        m.updatedAt = m.createdAt;
        return m;
    }

    @Override
    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public long getSizeBytes() { return sizeBytes; }
    public String getContentType() { return contentType; }
    public FileStatus getStatus() { return status; }

    public UUID getCurrentVersionId() { return currentVersionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFileName(String fileName) { this.fileName = fileName; touch(); }
    public void setContentType(String contentType) { this.contentType = contentType; touch(); }
    public void setStatus(FileStatus status) { this.status = status; touch(); }
    public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; touch(); }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; touch(); }

    private void touch() {
        this.updatedAt = Instant.now();
        if (this.createdAt == null) this.createdAt = this.updatedAt;
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
