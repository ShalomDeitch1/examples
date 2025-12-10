package com.example.directS3.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fileName;
    private long size;
    private String s3Key;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    public FileMetadata() {
    }

    public FileMetadata(String fileName, long size, String s3Key, FileStatus status) {
        this.fileName = fileName;
        this.size = size;
        this.s3Key = s3Key;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }
}
