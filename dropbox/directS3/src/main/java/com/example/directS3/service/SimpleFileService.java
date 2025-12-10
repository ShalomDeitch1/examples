package com.example.directS3.service;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.model.FileStatus;
import com.example.directS3.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class SimpleFileService {

    private final FileMetadataRepository repository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public SimpleFileService(FileMetadataRepository repository, S3Client s3Client) {
        this.repository = repository;
        this.s3Client = s3Client;
    }

    public FileMetadata uploadFile(String fileName, long size, InputStream inputStream) throws IOException {
        String s3Key = UUID.randomUUID().toString();

        // 1. Upload to S3 (Blocking)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, size));

        // 2. Save Metadata
        FileMetadata metadata = new FileMetadata(fileName, size, s3Key, FileStatus.AVAILABLE);
        return repository.save(metadata);
    }

    public FileMetadata getFileMetadata(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
    }
}
