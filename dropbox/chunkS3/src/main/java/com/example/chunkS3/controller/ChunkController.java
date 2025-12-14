package com.example.chunkS3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chunks")
@Deprecated
public class ChunkController {

    @PostMapping
    public ResponseEntity<String> uploadDisabled() {
        return ResponseEntity.status(410).body("/api/chunks is removed in Stage 3. Use /api/files + presigned URLs and client-side chunking.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> manifestDisabled() {
        return ResponseEntity.status(410).body("/api/chunks is removed in Stage 3. Use GET /api/files/{fileId}/manifest.");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<String> downloadDisabled() {
        return ResponseEntity.status(410).body("/api/chunks is removed in Stage 3. Use client-side reassembly from the manifest.");
    }
}
