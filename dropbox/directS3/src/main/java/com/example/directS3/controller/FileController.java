package com.example.directS3.controller;

import com.example.directS3.model.FileStatus;
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
        return ResponseEntity.ok(fileService.initUpload(fileName, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileService.DownloadResponse> downloadFile(@PathVariable UUID id) {
        return ResponseEntity.ok(fileService.getDownloadUrl(id));
    }

    // Manual completion for simulation purposes
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeUpload(@PathVariable UUID id) {
        fileService.updateStatus(id, FileStatus.AVAILABLE);
        return ResponseEntity.ok().build();
    }

    // Webhook for S3 Event Notification (simplified)
    @PostMapping("/webhook/s3")
    public ResponseEntity<Void> handleS3Event(@RequestBody Map<String, Object> event) {
        // Parse event to get keys and update status
        // implementation omitted for brevity in design, but would go here.
        // For this stage, manual completion or just 'uploading' is the focus.
        return ResponseEntity.ok().build();
    }
}
