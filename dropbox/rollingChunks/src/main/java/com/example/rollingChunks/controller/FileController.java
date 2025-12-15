package com.example.rollingChunks.controller;

import com.example.rollingChunks.model.ChunkingStrategy;
import com.example.rollingChunks.service.ChunkedFileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final ChunkedFileService service;

    public FileController(ChunkedFileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FileSummaryDto>> list() {
        var files = service.listFiles().stream()
                .map(f -> new FileSummaryDto(f.getId(), f.getFileName(), f.getContentType(), f.getSizeBytes(), f.getStatus(), f.getCurrentVersionId()))
                .toList();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/init")
    public ResponseEntity<ChunkedFileService.InitUploadResponse> init(@Valid @RequestBody InitUploadDto dto) {
        var req = new ChunkedFileService.InitUploadRequest(
                dto.fileId(),
                dto.fileName(),
                dto.contentType(),
                dto.chunkingStrategy(),
                dto.textNewlinesNormalized(),
                dto.endsWithNewline(),
                dto.reassembledSizeBytes(),
                dto.parts().stream().map(p -> new ChunkedFileService.InitPart(p.index(), p.hash(), p.lengthBytes())).toList()
        );
        return ResponseEntity.ok(service.initUpload(req));
    }

    @PostMapping("/{fileId}/versions/{versionId}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID fileId, @PathVariable UUID versionId) {
        try {
            service.completeUpload(fileId, versionId);
            return ResponseEntity.ok().build();
        } catch (ChunkedFileService.MissingChunksException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getResponse());
        }
    }

    @GetMapping("/{fileId}/manifest")
    public ResponseEntity<ChunkedFileService.ManifestResponse> manifest(@PathVariable UUID fileId) {
        return ResponseEntity.ok(service.manifest(fileId));
    }

    public record FileSummaryDto(UUID id, String fileName, String contentType, long sizeBytes,
                                 com.example.rollingChunks.model.FileStatus status, UUID currentVersionId) {}

    public record InitUploadDto(
            UUID fileId,
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull ChunkingStrategy chunkingStrategy,
            boolean textNewlinesNormalized,
            boolean endsWithNewline,
            @Min(0) long reassembledSizeBytes,
            @NotEmpty List<@Valid PartDto> parts
    ) {}

    public record PartDto(
            @Min(0) int index,
            @NotBlank String hash,
            @Min(0) int lengthBytes
    ) {}
}
