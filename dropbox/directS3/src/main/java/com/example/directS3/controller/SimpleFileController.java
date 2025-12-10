package com.example.directS3.controller;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.service.SimpleFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/simple/files")
public class SimpleFileController {

    private final SimpleFileService simpleFileService;

    public SimpleFileController(SimpleFileService simpleFileService) {
        this.simpleFileService = simpleFileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileMetadata metadata = simpleFileService.uploadFile(file.getOriginalFilename(), file.getSize(),
                file.getInputStream());
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileMetadata> getFile(@PathVariable UUID id) {
        return ResponseEntity.ok(simpleFileService.getFileMetadata(id));
    }
}
