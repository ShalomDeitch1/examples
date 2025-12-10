package com.example.dropbox.simplest.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private long fileSize;
    private String s3Key;
    private LocalDateTime uploadTime;

    protected FileMetadata() {
    }

    public FileMetadata(String fileName, long fileSize, String s3Key) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.s3Key = s3Key;
        this.uploadTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getS3Key() {
        return s3Key;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
}
