package com.example.chunkS3;

import com.example.chunkS3.repository.FileMetadataRepository;
import com.example.chunkS3.service.ChunkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
class ChunkServiceTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3));
        registry.add("aws.s3.region", localstack::getRegion);
        registry.add("aws.accessKeyId", localstack::getAccessKey);
        registry.add("aws.secretAccessKey", localstack::getSecretKey);
        registry.add("aws.s3.bucket", () -> "dropbox-stage3-test");
    }

    @Autowired
    ChunkService chunkService;

    @Autowired
    FileMetadataRepository repository;

    @Autowired
    S3Client s3Client;

    @BeforeEach
    void ensureBucket() {
        var bucket = "dropbox-stage3-test";
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        repository.deleteAll();
    }

    @Test
    void splitsIntoReadableChunksAndStoresInS3() {
        String text = "Chunking shows how data is split across multiple small pieces so you can verify every chunk easily.";
        var response = chunkService.uploadChunks(new ChunkService.UploadRequest("notes.txt", text, "text/plain"));

        assertThat(response.chunkKeys()).hasSizeGreaterThan(9); // roughly 100 chars / 10
        assertThat(response.chunkKeys()).allSatisfy(key -> assertThat(key).contains("chunks/"));

        // Each stored chunk should be <= 10 characters and readable
        var manifest = chunkService.manifest(response.fileId());
        assertThat(manifest.chunkKeys()).isNotEmpty();
        for (String key : manifest.chunkKeys()) {
            String content = s3Client.getObjectAsBytes(b -> b.bucket("dropbox-stage3-test").key(key)).asUtf8String();
            assertThat(content).isNotBlank();
            assertThat(content.length()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    void downloadReassemblesOriginalText() {
        String text = "Readable chunks make verification simple and transparent for teaching purposes.";
        var response = chunkService.uploadChunks(new ChunkService.UploadRequest("lesson.txt", text, "text/plain"));

        var download = chunkService.download(response.fileId());
        assertThat(download.content()).isEqualTo(text);
        assertThat(download.chunkCount()).isEqualTo(response.chunkKeys().size());
    }
}
