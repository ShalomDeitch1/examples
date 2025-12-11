package com.example.directS3.service;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.model.FileStatus;
import com.example.directS3.repository.FileMetadataRepository;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository repository;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public FileService(FileMetadataRepository repository, S3Presigner s3Presigner) {
        this.repository = repository;
        this.s3Presigner = s3Presigner;
    }

    public UploadResponse initUpload(String fileName, long size) {
        return initUpload(fileName, size, null);
    }

    // Overload to accept optional contentType from client
    public UploadResponse initUpload(String fileName, long size, String contentType) {
        String s3Key = UUID.randomUUID().toString();

        FileMetadata metadata = new FileMetadata(fileName, size, s3Key, FileStatus.PENDING);
        if (contentType != null) metadata.setContentType(contentType);
        metadata = repository.save(metadata);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        URL presignedUrl = s3Presigner.presignPutObject(presignRequest).url();
        log.info("Generated presigned PUT URL: {}", presignedUrl);

        return new UploadResponse(metadata.getId(), presignedUrl.toString());
    }

    public DownloadResponse getDownloadUrl(UUID id) {
        FileMetadata metadata = repository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));

        if (metadata.getStatus() != FileStatus.AVAILABLE) {
            // In a real system, you might handle this differently allow download if it
            // exists in S3 even if DB says pending?
            // For now, strict check.
            throw new RuntimeException("File is not available yet");
        }

        // Prefer stored contentType from metadata, otherwise infer from filename
        String contentType = metadata.getContentType();
        if (contentType == null) {
            contentType = java.net.URLConnection.guessContentTypeFromName(metadata.getFileName());
            if (contentType == null) contentType = "application/octet-stream";
        }

        GetObjectRequest objectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(metadata.getS3Key())
            .responseContentType(contentType)
            .responseContentDisposition("attachment; filename=\"" + metadata.getFileName() + "\"")
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(objectRequest)
            .build();

        URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();

        return new DownloadResponse(metadata, presignedUrl.toString());
    }

    // This method would be called by the S3 Event Notification listener
    public void markAsAvailable(String s3Key) {
        var maybe = repository.findFirstByS3Key(s3Key);
        FileMetadata metadata;
        if (maybe.isPresent()) {
            metadata = maybe.get();
            metadata.setStatus(FileStatus.AVAILABLE);
            log.info("Marked file as AVAILABLE: {} ({})", metadata.getFileName(), s3Key);
        } else {
            // Reconcile: create minimal metadata record if client skipped init
            // Use S3 key as fallback filename (happens if upload bypassed /init endpoint)
            String inferredName = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf("/") + 1) : s3Key;
            metadata = new FileMetadata(inferredName, 0L, s3Key, FileStatus.AVAILABLE);
            log.warn("Reconciled orphaned S3 object: {} -> {}", s3Key, inferredName);
        }
        repository.save(metadata);
    }

    // Helper to update status directly for simulation if needed
    public void updateStatus(UUID id, FileStatus status) {
        FileMetadata metadata = repository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        metadata.setStatus(status);
        repository.save(metadata);
    }

    public record UploadResponse(UUID fileId, String uploadUrl) {
    }

    public record DownloadResponse(FileMetadata metadata, String downloadUrl) {
    }

    // Expose bucket name for components that need to interact directly with S3
    public String getBucketName() {
        return bucketName;
    }
}
