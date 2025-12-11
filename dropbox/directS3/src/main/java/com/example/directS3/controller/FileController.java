package com.example.directS3.controller;

import com.example.directS3.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload/init")
    public ResponseEntity<FileService.UploadResponse> initUpload(@RequestBody Map<String, Object> payload) {
        String fileName = (String) payload.get("fileName");
        long size = ((Number) payload.get("size")).longValue();
        String contentType = (String) payload.getOrDefault("contentType", null);
        return ResponseEntity.ok(fileService.initUpload(fileName, size, contentType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileService.DownloadResponse> downloadFile(@PathVariable UUID id) {
        return ResponseEntity.ok(fileService.getDownloadUrl(id));
    }

    // Completion is handled via S3 -> SNS -> SQS notifications; no manual endpoint.
}
