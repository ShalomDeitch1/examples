package com.example.chunkS3.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.FetchType;
import org.springframework.data.domain.Persistable;

@Entity
public class FileMetadata implements Persistable<UUID> {

    @Id
    private UUID id;

    private String fileName;

    private long size;

    private String contentType;

    private int chunkSize;

    private int chunkCount;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "file_chunks", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "chunk_key")
    private List<String> chunkKeys = new ArrayList<>();

    @Transient
    private boolean isNew = true;

    protected FileMetadata() {}

    public FileMetadata(UUID id, String fileName, long size, String contentType, int chunkSize, int chunkCount, FileStatus status, List<String> chunkKeys) {
        this.id = id != null ? id : UUID.randomUUID();
        this.fileName = fileName;
        this.size = size;
        this.contentType = contentType;
        this.chunkSize = chunkSize;
        this.chunkCount = chunkCount;
        this.status = status;
        this.chunkKeys = new ArrayList<>(chunkKeys);
    }

    public FileMetadata(String fileName, long size, String contentType, int chunkSize, int chunkCount, FileStatus status, List<String> chunkKeys) {
        this(null, fileName, size, contentType, chunkSize, chunkCount, status, chunkKeys);
    }

    @Override
    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public long getSize() { return size; }
    public String getContentType() { return contentType; }
    public int getChunkSize() { return chunkSize; }
    public int getChunkCount() { return chunkCount; }
    public FileStatus getStatus() { return status; }
    public List<String> getChunkKeys() { return chunkKeys; }

    public void setStatus(FileStatus status) { this.status = status; }

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
