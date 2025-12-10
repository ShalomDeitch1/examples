package com.example.directS3.service;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.model.FileStatus;
import com.example.directS3.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileService {

    private final FileMetadataRepository repository;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public FileService(FileMetadataRepository repository, S3Presigner s3Presigner) {
        this.repository = repository;
        this.s3Presigner = s3Presigner;
    }

    public UploadResponse initUpload(String fileName, long size) {
        String s3Key = UUID.randomUUID().toString();

        FileMetadata metadata = new FileMetadata(fileName, size, s3Key, FileStatus.PENDING);
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

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(metadata.getS3Key())
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
        // Find by s3Key not efficient without index/query, but doable.
        // Assuming we need to find the metadata.
        // For simplicity, let's assume we can find it.
        // Ideally, we'd store the custom metadata 'file-id' in S3 object to correlate
        // easily.
        // Or query by S3Key.
    }

    // Helper to update status directly for simulation if needed
    public void updateStatus(UUID id, FileStatus status) {
        FileMetadata metadata = repository.findById(id).orElseThrow();
        metadata.setStatus(status);
        repository.save(metadata);
    }

    public record UploadResponse(UUID fileId, String uploadUrl) {
    }

    public record DownloadResponse(FileMetadata metadata, String downloadUrl) {
    }
}
