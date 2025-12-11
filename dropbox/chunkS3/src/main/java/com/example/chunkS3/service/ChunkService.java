package com.example.chunkS3.service;

import com.example.chunkS3.model.FileMetadata;
import com.example.chunkS3.model.FileStatus;
import com.example.chunkS3.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkService {

    private static final Logger log = LoggerFactory.getLogger(ChunkService.class);

    private final FileMetadataRepository repository;
    private final S3Client s3Client;
    private final int maxChunkSize;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public ChunkService(FileMetadataRepository repository, S3Client s3Client,
                        @Value("${app.chunk.size:10}") int maxChunkSize) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.maxChunkSize = maxChunkSize;
    }

    public UploadResponse uploadChunks(UploadRequest request) {
        String content = request.content();
        String fileName = request.fileName();
        String contentType = request.contentType();

        List<String> chunks = splitContent(content);
        List<String> keys = new ArrayList<>();
        UUID chunkPrefix = UUID.randomUUID();

        for (int i = 0; i < chunks.size(); i++) {
            String key = "chunks/" + chunkPrefix + "/" + i + ".txt";
            putChunk(key, chunks.get(i));
            keys.add(key);
        }

        FileMetadata metadata = new FileMetadata(
            fileName,
            content.getBytes(StandardCharsets.UTF_8).length,
            contentType,
            maxChunkSize,
            chunks.size(),
            FileStatus.AVAILABLE,
            keys
        );
        FileMetadata saved = repository.save(metadata);

        return new UploadResponse(saved.getId(), keys, maxChunkSize);
    }

    public ManifestResponse manifest(UUID id) {
        FileMetadata metadata = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
        return new ManifestResponse(metadata.getId(), metadata.getFileName(), metadata.getChunkKeys(), metadata.getChunkSize());
    }

    public DownloadResponse download(UUID id) {
        FileMetadata metadata = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
        StringBuilder sb = new StringBuilder();
        for (String key : metadata.getChunkKeys()) {
            String chunk = fetchChunk(key);
            sb.append(chunk);
        }
        return new DownloadResponse(metadata.getId(), metadata.getFileName(), sb.toString(), metadata.getChunkKeys().size());
    }

    private List<String> splitContent(String content) {
        List<String> result = new ArrayList<>();
        String[] lines = content.split("\\r?\\n", -1);
        for (String line : lines) {
            if (line.length() == 0) {
                result.add("");
                continue;
            }
            if (line.length() <= maxChunkSize) {
                result.add(line);
            } else {
                int start = 0;
                while (start < line.length()) {
                    int end = Math.min(start + maxChunkSize, line.length());
                    result.add(line.substring(start, end));
                    start = end;
                }
            }
        }
        return result;
    }

    private void putChunk(String key, String chunk) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain")
                .build();
        s3Client.putObject(req, RequestBody.fromString(chunk, StandardCharsets.UTF_8));
        log.debug("Stored chunk {} bytes at {}", chunk.length(), key);
    }

    private String fetchChunk(String key) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(req).asString(StandardCharsets.UTF_8);
    }

    public record UploadRequest(String fileName, String content, String contentType) {
        public UploadRequest {
            if (!StringUtils.hasText(fileName)) throw new IllegalArgumentException("fileName is required");
            if (content == null) throw new IllegalArgumentException("content is required");
        }
    }

    public record UploadResponse(UUID fileId, List<String> chunkKeys, int chunkSize) {}
    public record ManifestResponse(UUID fileId, String fileName, List<String> chunkKeys, int chunkSize) {}
    public record DownloadResponse(UUID fileId, String fileName, String content, int chunkCount) {}
}
