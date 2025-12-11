package com.example.chunkS3.controller;

import com.example.chunkS3.service.ChunkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chunks")
public class ChunkController {

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @PostMapping
    public ResponseEntity<ChunkService.UploadResponse> upload(@Valid @RequestBody UploadDto dto) {
        var req = new ChunkService.UploadRequest(dto.fileName(), dto.content(), dto.contentType());
        return ResponseEntity.ok(chunkService.uploadChunks(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChunkService.ManifestResponse> manifest(@PathVariable UUID id) {
        return ResponseEntity.ok(chunkService.manifest(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ChunkService.DownloadResponse> download(@PathVariable UUID id) {
        return ResponseEntity.ok(chunkService.download(id));
    }

    public record UploadDto(@NotBlank String fileName, @NotNull String content, String contentType) {}
}
