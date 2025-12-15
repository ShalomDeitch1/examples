package com.example.rollingChunks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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

import com.example.rollingChunks.model.ChunkingStrategy;
import com.example.rollingChunks.model.FileStatus;
import com.example.rollingChunks.repository.FileMetadataRepository;
import com.example.rollingChunks.service.ChunkedFileService;

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
        registry.add("aws.sns.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.SNS));
        registry.add("aws.sqs.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS));
        registry.add("aws.s3.region", localstack::getRegion);
        registry.add("aws.accessKeyId", localstack::getAccessKey);
        registry.add("aws.secretAccessKey", localstack::getSecretKey);
        registry.add("aws.s3.bucket", () -> "dropbox-stage4-test");
        registry.add("app.chunk.object-prefix", () -> "chunks/sha256/");
        registry.add("app.presign.ttl-seconds", () -> 600);
    }

    @Autowired
    ChunkedFileService chunkedFileService;

    @Autowired
    FileMetadataRepository repository;

    @Autowired
    S3Client s3Client;

    @BeforeEach
    void ensureBucket() {
        var bucket = "dropbox-stage4-test";
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
            s3Client.putObject(b -> b.bucket("dropbox-stage4-test").key(key), RequestBody.fromBytes(c.bytes()));
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
        s3Client.putObject(b -> b.bucket("dropbox-stage4-test").key("chunks/sha256/" + missing.hash()), RequestBody.fromBytes(bytes));

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

    @Test
    void rollingChunking_endToEndUploadAndReassemble() throws Exception {
        String text = ("alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo lima mike november oscar papa\n"
                + "repeat repeat repeat repeat repeat repeat repeat repeat repeat repeat\n"
                + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\n"
                + "the quick brown fox jumps over the lazy dog\n").repeat(40);

        var plan = rollingTextParts(text);

        var init = chunkedFileService.initUpload(new ChunkedFileService.InitUploadRequest(
                null,
                "rolling.txt",
                "text/plain",
                ChunkingStrategy.ROLLING_TEXT_NORMALIZED_LF,
                true,
                plan.endsWithNewline(),
                plan.reassembledSizeBytes(),
                plan.parts()
        ));

        assertThat(init.missingParts()).hasSize(init.expectedUniqueChunks());
        assertThat(init.expectedUniqueChunks()).isGreaterThan(1);

        for (var c : plan.chunks()) {
            String key = "chunks/sha256/" + c.hash();
            s3Client.putObject(b -> b.bucket("dropbox-stage4-test").key(key), RequestBody.fromBytes(c.bytes()));
        }

        chunkedFileService.completeUpload(init.fileId(), init.versionId());
        awaitAvailable(init.fileId(), Duration.ofSeconds(15));

        var manifest = chunkedFileService.manifest(init.fileId());
        assertThat(manifest.chunkingStrategy()).isEqualTo(ChunkingStrategy.ROLLING_TEXT_NORMALIZED_LF);

        String reassembled = reassembleRollingTextFromS3(manifest);
        assertThat(reassembled).isEqualTo(text.replace("\r\n", "\n"));
    }

    private long countChunkObjects() {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket("dropbox-stage4-test")
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
            byte[] bytes = s3Client.getObjectAsBytes(b -> b.bucket("dropbox-stage4-test").key("chunks/sha256/" + part.hash())).asByteArray();
            sb.append(new String(bytes, StandardCharsets.UTF_8));
            if (i < manifest.parts().size() - 1 || manifest.endsWithNewline()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String reassembleRollingTextFromS3(ChunkedFileService.ManifestResponse manifest) {
        var out = new StringBuilder();
        for (var part : manifest.parts()) {
            byte[] bytes = s3Client.getObjectAsBytes(b -> b.bucket("dropbox-stage4-test").key("chunks/sha256/" + part.hash())).asByteArray();
            out.append(new String(bytes, StandardCharsets.UTF_8));
        }
        return out.toString();
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

    // Minimal rolling/content-defined chunker for tests.
    // Note: The server does not validate chunk boundaries; it only uses the provided hashes/lengths.
    // This exists so the test exercises the Stage 4 strategy end-to-end.
    private static TextParts rollingTextParts(String text) throws Exception {
        String normalized = text.replace("\r\n", "\n");
        boolean endsWithNewline = normalized.endsWith("\n");
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);

        final int minBytes = 64;
        final int avgBytes = 256;
        final int maxBytes = 1024;

        int mask = nextPow2(avgBytes) - 1;

        List<int[]> cuts = new ArrayList<>();
        int last = 0;
        long h = 0;
        for (int i = 0; i < bytes.length; i++) {
            h = ((h << 1) + (GEAR[bytes[i] & 0xff] & 0xffffffffL)) & 0xffffffffL;
            int size = (i + 1) - last;
            if (size < minBytes) continue;
            if (size >= maxBytes || (((int) h) & mask) == 0) {
                cuts.add(new int[]{last, i + 1});
                last = i + 1;
                h = 0;
            }
        }
        if (last < bytes.length) cuts.add(new int[]{last, bytes.length});

        List<ChunkedFileService.InitPart> parts = new ArrayList<>();
        List<ChunkWithBytes> chunks = new ArrayList<>();

        int idx = 0;
        for (int[] c : cuts) {
            int start = c[0];
            int end = c[1];
            byte[] slice = java.util.Arrays.copyOfRange(bytes, start, end);
            String hash = sha256Hex(slice);
            parts.add(new ChunkedFileService.InitPart(idx, hash, slice.length));
            chunks.add(new ChunkWithBytes(idx, hash, slice.length, slice));
            idx++;
        }

        return new TextParts(endsWithNewline, bytes.length, parts, chunks);
    }

    private static int nextPow2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    // Deterministic gear table (mirrors the JS idea; exact values aren't important for the server).
    private static final int[] GEAR = buildGear();

    private static int[] buildGear() {
        int[] base = new int[]{
                0x1f123bb5,0x9e3779b9,0x7f4a7c15,0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,
                0x9b05688c,0x1d83d9ab,0x5be0cd19,0x243f6a88,0x85a308d3,0x13198a2e,0x03707344,0xa4093822,
                0x299f31d0,0x082efa98,0xec4e6c89,0x452821e6,0x38d01377,0xbe5466cf,0x34e90c6c,0xc0ac29b7,
                0xc97c50dd,0x3f84d5b5,0xb5470917,0x9216d5d9,0x8979fb1b,0xd1310ba6,0x98dfb5ac,0x2ffd72db,
                0xd01adfb7,0xb8e1afed,0x6a267e96,0xba7c9045,0xf12c7f99,0x24a19947,0xb3916cf7,0x0801f2e2,
                0x858efc16,0x636920d8,0x71574e69,0xa458fea3,0xf4933d7e,0x0d95748f,0x728eb658,0x718bcd58,
                0x82154aee,0x7b54a41d,0xc25a59b5,0x9c30d539,0x2af26013,0xc5d1b023,0x286085f0,0xca417918,
                0xb8db38ef,0x8e79dcb0,0x603a180e,0x6c9e0e8b,0xb01e8a3e,0xd71577c1,0xbd314b27,0x78af2fda,
        };
        int[] out = new int[256];
        int v = 0x9e3779b9;
        for (int i = 0; i < 256; i++) {
            v = v + 0x7f4a7c15;
            int b = base[i % base.length];
            int mix = (v << 13) | (v >>> 19);
            out[i] = b ^ mix;
        }
        return out;
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
