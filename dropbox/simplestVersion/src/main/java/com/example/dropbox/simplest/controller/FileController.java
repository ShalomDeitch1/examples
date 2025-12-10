package com.example.dropbox.simplest.controller;

import com.example.dropbox.simplest.model.FileMetadata;
import com.example.dropbox.simplest.service.FileService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable Long id) {
        FileMetadata metadata = fileService.getFileMetadata(id);
        String presignedUrl = fileService.generatePresignedUrl(metadata.getS3Key());

        Map<String, Object> response = new HashMap<>();
        response.put("metadata", metadata);
        response.put("downloadUrl", presignedUrl);

        return ResponseEntity.ok(response);
    }
}
