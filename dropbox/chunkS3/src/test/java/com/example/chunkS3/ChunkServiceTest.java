package com.example.chunkS3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.chunkS3.model.ChunkingStrategy;
import com.example.chunkS3.model.FileStatus;
import com.example.chunkS3.repository.FileMetadataRepository;
import com.example.chunkS3.service.ChunkedFileService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@SpringBootTest
@Testcontainers
class ChunkServiceTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");

    @SuppressWarnings("resource")
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3));
        registry.add("aws.s3.region", localstack::getRegion);
        registry.add("aws.accessKeyId", localstack::getAccessKey);
        registry.add("aws.secretAccessKey", localstack::getSecretKey);
        registry.add("aws.s3.bucket", () -> "dropbox-stage3-test");
        registry.add("app.chunk.object-prefix", () -> "chunks/sha256/");
        registry.add("app.presign.ttl-seconds", () -> 600);

        // Prevent awspring SQS listener containers from starting in tests.
        registry.add("app.s3.notifications.sqs.enabled", () -> false);
    }

    @Autowired
    ChunkedFileService chunkedFileService;

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
    void lineBasedChunking_onlySecondLineChanges() throws Exception {
        String v1 = "line1\nline2\nline3\n";
        String v2 = "line1\nLINE2_CHANGED\nline3\n";

        var p1 = textLineParts(v1);
        var p2 = textLineParts(v2);

        assertThat(p1.parts()).hasSize(3);
        assertThat(p2.parts()).hasSize(3);

        assertThat(p1.parts().get(0).hash()).isEqualTo(p2.parts().get(0).hash());
        assertThat(p1.parts().get(2).hash()).isEqualTo(p2.parts().get(2).hash());
        assertThat(p1.parts().get(1).hash()).isNotEqualTo(p2.parts().get(1).hash());
    }

    @Test
    void onlyUploadsOneChunkWhenSecondLineChanges() throws Exception {
        String v1 = "alpha\nbravo\ncharlie\n";
        var v1Parts = textLineParts(v1);

        var init1 = chunkedFileService.initUpload(new ChunkedFileService.InitUploadRequest(
                null,
                "notes.txt",
                "text/plain",
                ChunkingStrategy.TEXT_LINES_NORMALIZED_LF,
                true,
                v1Parts.endsWithNewline(),
                v1Parts.reassembledSizeBytes(),
            v1Parts.parts()
        ));

        // Expect one missing part per unique chunk hash
        assertThat(init1.expectedUniqueChunks()).isEqualTo(3);
        assertThat(init1.missingParts()).hasSize(init1.expectedUniqueChunks());

        // Upload v1 chunks (direct put; still triggers S3 notifications).
        for (var c : v1Parts.chunks()) {
            String key = "chunks/sha256/" + c.hash();
            s3Client.putObject(b -> b.bucket("dropbox-stage3-test").key(key), RequestBody.fromBytes(c.bytes()));
        }
        try {
            chunkedFileService.completeUpload(init1.fileId(), init1.versionId());
        } catch (ChunkedFileService.MissingChunksException e) {
            throw new AssertionError("Unexpected missing chunks on first complete: " + e.getResponse().missingChunks(), e);
        }

        awaitAvailable(init1.fileId(), Duration.ofSeconds(15));
        long objectsAfterV1 = countChunkObjects();

        String v2 = "alpha\nBRAVO_CHANGED\ncharlie\n";
        var v2Parts = textLineParts(v2);

        var init2 = chunkedFileService.initUpload(new ChunkedFileService.InitUploadRequest(
                init1.fileId(),
                "notes.txt",
                "text/plain",
                ChunkingStrategy.TEXT_LINES_NORMALIZED_LF,
                true,
                v2Parts.endsWithNewline(),
                v2Parts.reassembledSizeBytes(),
            v2Parts.parts()
        ));

        assertThat(init2.expectedUniqueChunks()).isEqualTo(1);
        assertThat(init2.missingParts()).hasSize(init2.expectedUniqueChunks());
        assertThat(init2.status()).isEqualTo(FileStatus.UPDATING);

        // Upload only the missing chunk (second line).
        var missing = init2.missingParts().getFirst();
        var bytes = v2Parts.chunks().get(missing.index()).bytes();
        s3Client.putObject(b -> b.bucket("dropbox-stage3-test").key("chunks/sha256/" + missing.hash()), RequestBody.fromBytes(bytes));

        try {
            chunkedFileService.completeUpload(init2.fileId(), init2.versionId());
        } catch (ChunkedFileService.MissingChunksException e) {
            throw new AssertionError("Unexpected missing chunks on second complete: " + e.getResponse().missingChunks(), e);
        }
        awaitAvailable(init2.fileId(), Duration.ofSeconds(15));

        long objectsAfterV2 = countChunkObjects();
        assertThat(objectsAfterV2).isEqualTo(objectsAfterV1 + 1);

        // Verify download manifest reassembles to v2.
        var manifest = chunkedFileService.manifest(init2.fileId());
        assertThat(manifest.chunkingStrategy()).isEqualTo(ChunkingStrategy.TEXT_LINES_NORMALIZED_LF);

        String reassembled = reassembleTextFromS3(manifest);
        assertThat(reassembled).isEqualTo(v2.replace("\r\n", "\n"));
    }

    private long countChunkObjects() {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket("dropbox-stage3-test")
                .prefix("chunks/sha256/")
                .build())
            .contents()
            .stream()
            .map(o -> o.key())
            .distinct()
            .count();
    }

    private void awaitAvailable(UUID fileId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            var f = repository.findById(fileId).orElseThrow();
            if (f.getStatus() == FileStatus.AVAILABLE && f.getCurrentVersionId() != null) {
                return;
            }
            Thread.sleep(250);
        }
        var f = repository.findById(fileId).orElseThrow();
        throw new AssertionError("Timed out waiting for AVAILABLE; status=" + f.getStatus());
    }

    private String reassembleTextFromS3(ChunkedFileService.ManifestResponse manifest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < manifest.parts().size(); i++) {
            var part = manifest.parts().get(i);
            byte[] bytes = s3Client.getObjectAsBytes(b -> b.bucket("dropbox-stage3-test").key("chunks/sha256/" + part.hash())).asByteArray();
            sb.append(new String(bytes, StandardCharsets.UTF_8));
            if (i < manifest.parts().size() - 1 || manifest.endsWithNewline()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static TextParts textLineParts(String text) throws Exception {
        String normalized = text.replace("\r\n", "\n");
        boolean endsWithNewline = normalized.endsWith("\n");
        String[] raw = normalized.split("\n", -1);
        int count = endsWithNewline ? raw.length - 1 : raw.length;

        List<ChunkedFileService.InitPart> parts = new java.util.ArrayList<>();
        List<ChunkWithBytes> chunks = new java.util.ArrayList<>();

        long reassembledSize = 0;
        for (int i = 0; i < count; i++) {
            byte[] bytes = raw[i].getBytes(StandardCharsets.UTF_8);
            String hash = sha256Hex(bytes);
            parts.add(new ChunkedFileService.InitPart(i, hash, bytes.length));
            chunks.add(new ChunkWithBytes(i, hash, bytes.length, bytes));
            reassembledSize += bytes.length;
            if (i < count - 1 || endsWithNewline) reassembledSize += 1;
        }

        return new TextParts(endsWithNewline, reassembledSize, parts, chunks);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        return HexFormat.of().formatHex(digest);
    }

    private record ChunkWithBytes(int index, String hash, int lengthBytes, byte[] bytes) {}

    private record TextParts(boolean endsWithNewline,
                             long reassembledSizeBytes,
                             List<ChunkedFileService.InitPart> parts,
                             List<ChunkWithBytes> chunks) {}
}
