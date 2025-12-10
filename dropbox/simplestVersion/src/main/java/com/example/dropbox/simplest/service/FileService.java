package com.example.dropbox.simplest.service;

import com.example.dropbox.simplest.model.FileMetadata;
import com.example.dropbox.simplest.model.FileRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public FileService(S3Client s3Client, S3Presigner s3Presigner, FileRepository fileRepository) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.fileRepository = fileRepository;
    }

    // Stage 1: The 'Naive' Upload
    // Server acts as a proxy, receiving the stream and pushing to S3.
    public FileMetadata uploadFile(MultipartFile file) throws IOException {
        String s3Key = UUID.randomUUID().toString();

        // Blocking Upload to S3
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Save Metadata
        FileMetadata metadata = new FileMetadata(file.getOriginalFilename(), file.getSize(), s3Key);
        return fileRepository.save(metadata);
    }

    public FileMetadata getFileMetadata(Long id) {
        return fileRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
    }

    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(b -> b.bucket(bucketName).key(s3Key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
