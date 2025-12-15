package com.example.rollingChunks.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.rollingChunks.model.ChangeEvent;
import com.example.rollingChunks.model.ChangeEventType;
import com.example.rollingChunks.model.DeviceCheckpoint;
import com.example.rollingChunks.model.FileMetadata;
import com.example.rollingChunks.model.FileVersion;
import com.example.rollingChunks.repository.ChangeEventRepository;
import com.example.rollingChunks.repository.DeviceCheckpointRepository;

@Service
public class ChangeFeedService {

    private final ChangeEventRepository changeEventRepository;
    private final DeviceCheckpointRepository checkpointRepository;
    private final ChangeFeedNotifier notifier;

    public ChangeFeedService(ChangeEventRepository changeEventRepository,
                             DeviceCheckpointRepository checkpointRepository,
                             ChangeFeedNotifier notifier) {
        this.changeEventRepository = changeEventRepository;
        this.checkpointRepository = checkpointRepository;
        this.notifier = notifier;
    }

    /**
     * Durable, idempotent record of a file/version becoming AVAILABLE.
     * DB is source-of-truth; SNS is best-effort for "online" notification.
     */
    @Transactional
    public void recordFileAvailable(FileMetadata file, FileVersion version) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(version, "version");

        if (changeEventRepository.existsByEventTypeAndVersionId(ChangeEventType.FILE_AVAILABLE, version.getId())) {
            return;
        }

        ChangeEvent event = ChangeEvent.fileAvailable(file, version);
        ChangeEvent saved = changeEventRepository.save(event);
        notifier.publishBestEffort(saved);
    }

    @Transactional(readOnly = true)
    public ChangeFeedResponse getChanges(String deviceId, int limit) {
        validateDeviceId(deviceId);
        int cappedLimit = Math.max(1, Math.min(limit, 500));

        DeviceCheckpoint checkpoint = checkpointRepository.findById(deviceId)
                .orElse(DeviceCheckpoint.newDevice(deviceId));

        List<ChangeEvent> events = changeEventRepository.findAfter(
                checkpoint.getLastSeenEventId(),
                PageRequest.of(0, cappedLimit)
        );

        Long lastId = events.isEmpty() ? null : events.get(events.size() - 1).getId();
        long nextCursor = lastId == null ? checkpoint.getLastSeenEventId() : lastId;

        return new ChangeFeedResponse(
                deviceId,
                checkpoint.getLastSeenEventId(),
                nextCursor,
                events.stream().map(ChangeEventDto::from).toList()
        );
    }

    @Transactional
    public DeviceCheckpointResponse ack(String deviceId, long lastSeenEventId) {
        validateDeviceId(deviceId);
        if (lastSeenEventId < 0) throw new IllegalArgumentException("lastSeenEventId must be >= 0");

        DeviceCheckpoint checkpoint = checkpointRepository.findById(deviceId)
                .orElse(DeviceCheckpoint.newDevice(deviceId));
        checkpoint.advanceTo(lastSeenEventId);
        checkpointRepository.save(checkpoint);
        return new DeviceCheckpointResponse(deviceId, checkpoint.getLastSeenEventId(), checkpoint.getUpdatedAt());
    }

    private static void validateDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) throw new IllegalArgumentException("deviceId is required");
        if (deviceId.length() > 128) throw new IllegalArgumentException("deviceId is too long");
    }

    public record ChangeFeedResponse(
            String deviceId,
            long lastSeenEventId,
            long nextCursor,
            List<ChangeEventDto> events
    ) {}

        public record ChangeEventDto(
            Long id,
            String eventType,
            java.time.Instant createdAt,
            java.util.UUID fileId,
            java.util.UUID versionId,
            String fileName,
            String contentType,
            long sizeBytes
    ) {
        static ChangeEventDto from(ChangeEvent e) {
            return new ChangeEventDto(
                    e.getId(),
                    e.getEventType().name(),
                    e.getCreatedAt(),
                    e.getFileId(),
                    e.getVersionId(),
                    e.getFileName(),
                    e.getContentType(),
                    e.getSizeBytes()
            );
        }
    }

    public record DeviceCheckpointResponse(
            String deviceId,
            long lastSeenEventId,
            java.time.Instant updatedAt
    ) {}
}
