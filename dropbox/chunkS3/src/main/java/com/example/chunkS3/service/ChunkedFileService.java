package com.example.chunkS3.service;

import com.example.chunkS3.model.ChunkPart;
import com.example.chunkS3.model.ChunkingStrategy;
import com.example.chunkS3.model.FileMetadata;
import com.example.chunkS3.model.FileStatus;
import com.example.chunkS3.model.FileVersion;
import com.example.chunkS3.model.UploadSession;
import com.example.chunkS3.repository.FileMetadataRepository;
import com.example.chunkS3.repository.FileVersionRepository;
import com.example.chunkS3.repository.UploadSessionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ChunkedFileService {

    private static final Logger log = LoggerFactory.getLogger(ChunkedFileService.class);

    private final FileMetadataRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final UploadSessionRepository sessionRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${app.chunk.object-prefix:chunks/sha256/}")
    private String chunkObjectPrefix;

    @Value("${app.presign.ttl-seconds:600}")
    private long presignTtlSeconds;

    public ChunkedFileService(FileMetadataRepository fileRepository,
                             FileVersionRepository versionRepository,
                             UploadSessionRepository sessionRepository,
                             S3Client s3Client,
                             S3Presigner s3Presigner) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
        this.sessionRepository = sessionRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Transactional
    public InitUploadResponse initUpload(InitUploadRequest req) {
        validateInit(req);

        FileMetadata file;
        boolean updatingExisting;
        if (req.fileId() == null) {
            file = FileMetadata.newPending(req.fileName(), req.contentType());
            file = fileRepository.save(file);
            updatingExisting = false;
        } else {
            file = fileRepository.findById(req.fileId())
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + req.fileId()));
            updatingExisting = true;
            file.setFileName(req.fileName());
            file.setContentType(req.contentType());
            file.setStatus(FileStatus.UPDATING);
            fileRepository.save(file);
        }

        long reassembledSizeBytes = req.reassembledSizeBytes();
        List<ChunkPart> parts = req.parts().stream()
                .map(p -> new ChunkPart(p.hash(), p.lengthBytes()))
                .toList();

        FileStatus initialStatus = updatingExisting ? FileStatus.UPDATING : FileStatus.PENDING;
        FileVersion version = FileVersion.create(
                file.getId(),
                req.chunkingStrategy(),
                req.textNewlinesNormalized(),
                req.endsWithNewline(),
                reassembledSizeBytes,
                parts,
                initialStatus);
        version = versionRepository.save(version);

        // Expected part count should reflect unique chunk hashes that are not already present in S3
        UploadSession session = UploadSession.create(file.getId(), version, initialStatus, countMissingUnique(req.parts()));
        session = sessionRepository.save(session);

        Map<String, String> presignedPutByHash = new HashMap<>();
        List<MissingPart> missingParts = new ArrayList<>();

        // Determine missing per unique hash; if already exists we mark as received immediately.
        for (InitPart part : req.parts()) {
            String hash = part.hash();
            if (presignedPutByHash.containsKey(hash)) {
                if (!existsInS3(hash) && !session.hasReceived(hash)) {
                    missingParts.add(new MissingPart(part.index(), hash, part.lengthBytes(), presignedPutByHash.get(hash)));
                }
                continue;
            }

            if (existsInS3(hash)) {
                session.markReceived(hash);
            } else {
                String uploadUrl = presignPutUrl(hash);
                presignedPutByHash.put(hash, uploadUrl);
                missingParts.add(new MissingPart(part.index(), hash, part.lengthBytes(), uploadUrl));
            }
        }

        // Update size/status; if nothing missing, this session could be immediately complete after clientComplete.
        file.setSizeBytes(reassembledSizeBytes);
        fileRepository.save(file);
        sessionRepository.save(session);

        return new InitUploadResponse(file.getId(), version.getId(), session.getId(), file.getStatus(), missingParts,
                session.getReceivedPartCount(), session.getExpectedPartCount());
    }

    @Transactional
    public void completeUpload(UUID fileId, UUID versionId) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        FileVersion version = versionRepository.findByIdAndFileId(versionId, fileId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));
        UploadSession session = sessionRepository.findByVersion_Id(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found for version: " + versionId));

        session.markClientComplete();

        // Defensive reconciliation: S3 notifications are async and can be missed/delayed.
        // Before finalizing, verify all expected unique chunk hashes exist in S3.
        var missing = verifyAndMarkReceivedFromS3(version, session);
        if (!missing.isEmpty()) {
            sessionRepository.save(session);
            versionRepository.save(version);
            fileRepository.save(file);
            throw new MissingChunksException(new CompleteUploadMissingResponse(fileId, versionId, missing));
        }

        // Authoritative finalize: if the client says "complete" and all expected chunks exist in S3,
        // the version is available even if async notifications are delayed.
        finalizeAvailable(file, version, session);

        sessionRepository.save(session);
        versionRepository.save(version);
        fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileMetadata> listFiles() {
        return fileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ManifestResponse manifest(UUID fileId) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        UUID versionId = file.getCurrentVersionId();
        if (versionId == null) {
            throw new IllegalStateException("File has no available version yet");
        }
        FileVersion version = versionRepository.findByIdAndFileId(versionId, fileId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        if (file.getStatus() != FileStatus.AVAILABLE || version.getStatus() != FileStatus.AVAILABLE) {
            throw new IllegalStateException("File is not available yet");
        }

        List<ManifestPart> parts = new ArrayList<>();
        for (int i = 0; i < version.getParts().size(); i++) {
            ChunkPart part = version.getParts().get(i);
            String url = presignGetUrl(part.getHash());
            parts.add(new ManifestPart(i, part.getHash(), part.getLengthBytes(), url));
        }

        return new ManifestResponse(file.getId(), version.getId(), file.getFileName(), file.getContentType(),
                version.getChunkingStrategy(), version.isTextNewlinesNormalized(), version.isEndsWithNewline(),
                version.getReassembledSizeBytes(), parts);
    }

    @Transactional
    public void onObjectCreated(String keyFromEvent) {
        String decodedKey = urlDecodeKey(keyFromEvent);
        String hash = extractHashFromChunkKey(decodedKey);
        if (hash == null) {
            return;
        }

        Collection<FileStatus> active = List.of(FileStatus.PENDING, FileStatus.UPDATING);
        List<UploadSession> sessions = sessionRepository.findActiveSessionsExpectingHash(hash, active);
        if (sessions.isEmpty()) {
            return;
        }

        for (UploadSession session : sessions) {
            if (!session.hasReceived(hash)) {
                session.markReceived(hash);
            }

            FileVersion version = session.getVersion();
            FileMetadata file = fileRepository.findById(session.getFileId())
                    .orElseThrow(() -> new IllegalStateException("File missing for session: " + session.getId()));

            maybeFinalizeIfComplete(file, version, session);

            sessionRepository.save(session);
            versionRepository.save(version);
            fileRepository.save(file);
        }
    }

    private void maybeFinalizeIfComplete(FileMetadata file, FileVersion version, UploadSession session) {
        if (session.isClientComplete() && session.isAllReceived()) {
            finalizeAvailable(file, version, session);
        }
    }

    private static void finalizeAvailable(FileMetadata file, FileVersion version, UploadSession session) {
        session.setStatus(FileStatus.AVAILABLE);
        version.setStatus(FileStatus.AVAILABLE);
        file.setCurrentVersionId(version.getId());
        file.setStatus(FileStatus.AVAILABLE);
    }

    private List<MissingChunk> verifyAndMarkReceivedFromS3(FileVersion version, UploadSession session) {
        Set<String> uniqueHashes = version.getParts().stream()
                .map(ChunkPart::getHash)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));

        List<MissingChunk> missing = new ArrayList<>();
        for (String hash : uniqueHashes) {
            if (existsInS3(hash)) {
                session.markReceived(hash);
            } else {
                missing.add(new MissingChunk(hash, presignPutUrl(hash)));
            }
        }
        return missing;
    }

    private boolean existsInS3(String hash) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(chunkKey(hash)).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    private String presignPutUrl(String hash) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(chunkKey(hash))
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
                .putObjectRequest(put)
                .build();

        URL url = s3Presigner.presignPutObject(presign).url();
        return url.toString();
    }

    private String presignGetUrl(String hash) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(chunkKey(hash))
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
                .getObjectRequest(get)
                .build();

        URL url = s3Presigner.presignGetObject(presign).url();
        return url.toString();
    }

    private String chunkKey(String hash) {
        String prefix = chunkObjectPrefix;
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        return prefix + hash;
    }

    private static int uniqueHashCount(List<InitPart> parts) {
        Set<String> hashes = new HashSet<>();
        for (InitPart p : parts) {
            hashes.add(p.hash());
        }
        return hashes.size();
    }

    private int countMissingUnique(List<InitPart> parts) {
        Set<String> hashes = new HashSet<>();
        int missing = 0;
        for (InitPart p : parts) {
            String h = p.hash();
            if (hashes.add(h)) {
                if (!existsInS3(h)) missing++;
            }
        }
        return missing;
    }

    private static String urlDecodeKey(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return key;
        }
    }

    private String extractHashFromChunkKey(String key) {
        String prefix = chunkObjectPrefix;
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        if (!key.startsWith(prefix)) return null;
        String hash = key.substring(prefix.length());
        if (!StringUtils.hasText(hash)) return null;
        return hash;
    }

    private static void validateInit(InitUploadRequest req) {
        if (!StringUtils.hasText(req.fileName())) throw new IllegalArgumentException("fileName is required");
        if (!StringUtils.hasText(req.contentType())) throw new IllegalArgumentException("contentType is required");
        if (req.chunkingStrategy() == null) throw new IllegalArgumentException("chunkingStrategy is required");
        if (req.parts() == null || req.parts().isEmpty()) throw new IllegalArgumentException("parts is required");
        for (InitPart p : req.parts()) {
            if (p.index() < 0) throw new IllegalArgumentException("part.index must be >= 0");
            if (!StringUtils.hasText(p.hash())) throw new IllegalArgumentException("part.hash is required");
            if (p.lengthBytes() < 0) throw new IllegalArgumentException("part.lengthBytes must be >= 0");
        }
    }

    public record InitPart(int index, String hash, int lengthBytes) {}

    public record InitUploadRequest(
            UUID fileId,
            String fileName,
            String contentType,
            ChunkingStrategy chunkingStrategy,
            boolean textNewlinesNormalized,
            boolean endsWithNewline,
            long reassembledSizeBytes,
            List<InitPart> parts
    ) {}

    public record MissingPart(int index, String hash, int lengthBytes, String uploadUrl) {}

    public record InitUploadResponse(
            UUID fileId,
            UUID versionId,
            UUID sessionId,
            FileStatus status,
            List<MissingPart> missingParts,
            int receivedUniqueChunks,
            int expectedUniqueChunks
    ) {}

    public record ManifestPart(int index, String hash, int lengthBytes, String downloadUrl) {}

    public record ManifestResponse(
            UUID fileId,
            UUID versionId,
            String fileName,
            String contentType,
            ChunkingStrategy chunkingStrategy,
            boolean textNewlinesNormalized,
            boolean endsWithNewline,
            long reassembledSizeBytes,
            List<ManifestPart> parts
    ) {}

    public record MissingChunk(String hash, String uploadUrl) {}

    public record CompleteUploadMissingResponse(UUID fileId, UUID versionId, List<MissingChunk> missingChunks) {}

    public static class MissingChunksException extends RuntimeException {
        private final CompleteUploadMissingResponse response;

        public MissingChunksException(CompleteUploadMissingResponse response) {
            super("Missing chunks");
            this.response = response;
        }

        public CompleteUploadMissingResponse getResponse() {
            return response;
        }
    }
}
