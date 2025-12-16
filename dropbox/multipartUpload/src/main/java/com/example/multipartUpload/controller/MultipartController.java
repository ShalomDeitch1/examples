package com.example.multipartUpload.controller;

import com.example.multipartUpload.service.MultipartUploadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/multipart")
public class MultipartController {

    private final MultipartUploadService service;

    public MultipartController(MultipartUploadService service) {
        this.service = service;
    }

    public record InitRequest(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @Min(0) long sizeBytes
    ) {}

    public record InitResponse(
            String uploadId,
            String objectKey,
            long partSizeBytes
    ) {}

    @PostMapping("/init")
    public InitResponse init(@Valid @RequestBody InitRequest req) {
        var r = service.init(req.fileName(), req.contentType(), req.sizeBytes());
        return new InitResponse(r.uploadId(), r.objectKey(), r.partSizeBytes());
    }

    public record PresignPartRequest(
            @NotBlank String uploadId,
            @NotBlank String objectKey,
            @Min(1) int partNumber
    ) {}

    public record PresignPartResponse(String uploadUrl) {}

    @PostMapping("/presign")
    public PresignPartResponse presign(@Valid @RequestBody PresignPartRequest req) {
        return new PresignPartResponse(service.presignUploadPart(req.uploadId(), req.objectKey(), req.partNumber()));
    }

    public record CompletePart(
            @Min(1) int partNumber,
            @NotBlank String eTag
    ) {}

    public record CompleteRequest(
            @NotBlank String uploadId,
            @NotBlank String objectKey,
            @NotNull List<@Valid CompletePart> parts
    ) {}

    public record CompleteResponse(String downloadUrl) {}

    @PostMapping("/complete")
    public CompleteResponse complete(@Valid @RequestBody CompleteRequest req) {
        service.complete(req.uploadId(), req.objectKey(), req.parts());
        return new CompleteResponse(service.presignDownload(req.objectKey()));
    }

    public record AbortRequest(
            @NotBlank String uploadId,
            @NotBlank String objectKey
    ) {}

    @PostMapping("/abort")
    public void abort(@Valid @RequestBody AbortRequest req) {
        service.abort(req.uploadId(), req.objectKey());
    }
}
