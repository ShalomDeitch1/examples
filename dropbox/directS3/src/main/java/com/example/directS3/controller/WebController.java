package com.example.directS3.controller;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.model.FileStatus;
import com.example.directS3.repository.FileMetadataRepository;
import com.example.directS3.service.FileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Controller
public class WebController {

    private final FileMetadataRepository repository;
    private final S3Client s3Client;
    private final FileService fileService;

    public WebController(FileMetadataRepository repository, S3Client s3Client, FileService fileService) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.fileService = fileService;
    }

    @GetMapping("/ui")
    public String list(Model model) {
        List<FileMetadata> files = repository.findAll();
        model.addAttribute("files", files);
        return "ui/list";
    }

    @GetMapping("/ui/upload")
    public String uploadForm() {
        return "ui/upload";
    }

    @PostMapping("/ui/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file) throws Exception {
        String key = UUID.randomUUID().toString();
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(fileService.bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(por, RequestBody.fromBytes(file.getBytes()));

        FileMetadata meta = new FileMetadata(file.getOriginalFilename(), file.getSize(), key, FileStatus.AVAILABLE);
        repository.save(meta);

        return "redirect:/ui";
    }

    @GetMapping("/ui/download/{id}")
    public String download(@PathVariable UUID id) {
        var resp = fileService.getDownloadUrl(id);
        return "redirect:" + resp.downloadUrl();
    }

    @GetMapping("/ui/view/{id}")
    public String view(@PathVariable UUID id, Model model) throws Exception {
        var resp = fileService.getDownloadUrl(id);
        // fetch content
        try (InputStream in = java.net.URI.create(resp.downloadUrl()).toURL().openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            String content = new String(out.toByteArray(), StandardCharsets.UTF_8);
            model.addAttribute("content", content);
        }
        model.addAttribute("fileId", id);
        return "ui/view";
    }
}
