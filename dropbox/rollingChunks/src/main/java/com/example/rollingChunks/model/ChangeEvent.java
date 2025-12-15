package com.example.rollingChunks.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "change_event",
        indexes = {
                @Index(name = "idx_change_event_created_at", columnList = "createdAt"),
                @Index(name = "idx_change_event_version", columnList = "versionId"),
                @Index(name = "idx_change_event_type_version", columnList = "eventType,versionId")
        }
)
public class ChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChangeEventType eventType;

    private Instant createdAt;

    @Column(nullable = false)
    private UUID fileId;

    @Column(nullable = false)
    private UUID versionId;

    private String fileName;

    private String contentType;

    private long sizeBytes;

    protected ChangeEvent() {}

    public static ChangeEvent fileAvailable(FileMetadata file, FileVersion version) {
        ChangeEvent e = new ChangeEvent();
        e.eventType = ChangeEventType.FILE_AVAILABLE;
        e.createdAt = Instant.now();
        e.fileId = file.getId();
        e.versionId = version.getId();
        e.fileName = file.getFileName();
        e.contentType = file.getContentType();
        e.sizeBytes = file.getSizeBytes();
        return e;
    }

    public Long getId() {
        return id;
    }

    public ChangeEventType getEventType() {
        return eventType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getFileId() {
        return fileId;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
