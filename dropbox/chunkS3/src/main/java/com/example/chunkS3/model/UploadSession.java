package com.example.chunkS3.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class UploadSession implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private FileVersion version;

    private UUID fileId;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    private int expectedPartCount;

    private int receivedPartCount;

    private boolean clientComplete;

    private Instant createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "upload_session_received", joinColumns = @JoinColumn(name = "session_id"))
    private Set<String> receivedHashes = new HashSet<>();

    @Transient
    private boolean isNew = true;

    protected UploadSession() {}

    public static UploadSession create(UUID fileId, FileVersion version, FileStatus status, int expectedPartCount) {
        UploadSession s = new UploadSession();
        s.id = UUID.randomUUID();
        s.fileId = fileId;
        s.version = version;
        s.status = status;
        s.expectedPartCount = expectedPartCount;
        s.receivedPartCount = 0;
        s.clientComplete = false;
        s.createdAt = Instant.now();
        return s;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public FileVersion getVersion() {
        return version;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public int getExpectedPartCount() {
        return expectedPartCount;
    }

    public int getReceivedPartCount() {
        return receivedPartCount;
    }

    public boolean isClientComplete() {
        return clientComplete;
    }

    public void markClientComplete() {
        this.clientComplete = true;
    }

    public boolean hasReceived(String hash) {
        return receivedHashes.contains(hash);
    }

    public boolean markReceived(String hash) {
        if (receivedHashes.add(hash)) {
            receivedPartCount++;
            return true;
        }
        return false;
    }

    public boolean isAllReceived() {
        return receivedPartCount >= expectedPartCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
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
