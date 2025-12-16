package com.example.multipartUpload.service;

import com.example.multipartUpload.controller.MultipartController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

@Service
public class MultipartUploadService {

        private final S3Client s3;
        private final S3Presigner presigner;

        private final String bucket;
        private final long partSizeBytes;
        private final String objectPrefix;

        public MultipartUploadService(
                        S3Client s3,
                        S3Presigner presigner,
                        @Value("${aws.s3.bucket}") String bucket,
                        @Value("${app.multipart.part-size-bytes}") long partSizeBytes,
                        @Value("${app.multipart.object-prefix:uploads/}") String objectPrefix) {
                this.s3 = s3;
                this.presigner = presigner;
                this.bucket = bucket;
                this.partSizeBytes = partSizeBytes;
                this.objectPrefix = objectPrefix;
        }

        public record InitResult(String uploadId, String objectKey, long partSizeBytes) {
        }

        public InitResult init(String fileName, String contentType, long sizeBytes) {
                ensureBucketCorsForBrowserEtags();

                String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                String keyPrefix = objectPrefix.endsWith("/") ? objectPrefix : (objectPrefix + "/");
                String objectKey = keyPrefix + UUID.randomUUID() + "/" + safeName;

                var resp = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .contentType(contentType)
                                .build());

                return new InitResult(resp.uploadId(), objectKey, partSizeBytes);
        }

        public String presignUploadPart(String uploadId, String objectKey, int partNumber) {
                var uploadPartRequest = UploadPartRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build();

                PresignedUploadPartRequest presigned = presigner.presignUploadPart(b -> b
                                .signatureDuration(Duration.ofMinutes(10))
                                .uploadPartRequest(uploadPartRequest));

                URL url = presigned.url();
                return url.toString();
        }

        public void complete(String uploadId, String objectKey, List<MultipartController.CompletePart> parts) {
                var completedParts = parts.stream()
                                .map(p -> CompletedPart.builder()
                                                .partNumber(p.partNumber())
                                                .eTag(normalizeEtag(p.eTag()))
                                                .build())
                                .toList();

                s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .uploadId(uploadId)
                                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                                .build());
        }

        public void abort(String uploadId, String objectKey) {
                s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .uploadId(uploadId)
                                .build());
        }

        public String presignDownload(String objectKey) {
                PresignedGetObjectRequest req = presigner.presignGetObject(b -> b
                                .signatureDuration(Duration.ofMinutes(10))
                                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build()));
                return req.url().toString();
        }

        private static String normalizeEtag(String eTag) {
                String t = eTag.trim();
                if (t.startsWith("\"") && t.endsWith("\""))
                        return t;
                // Some clients may strip quotes; AWS accepts quoted ETag values.
                return "\"" + t.replace("\"", "") + "\"";
        }

        private void ensureBucketCorsForBrowserEtags() {
                var rule = CORSRule.builder()
                                .allowedMethods("GET", "PUT", "POST", "HEAD")
                                .allowedOrigins("*")
                                .allowedHeaders("*")
                                .exposeHeaders("ETag")
                                .maxAgeSeconds(3600)
                                .build();

                try {
                        s3.putBucketCors(PutBucketCorsRequest.builder()
                                        .bucket(bucket)
                                        .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build())
                                        .build());
                } catch (Exception ignored) {
                        // LocalStack/bucket may not exist yet; the shared runner creates it.
                        // Swallowing the exception is safe because retries will attempt to create the
                        // CORS rule again.
                }
        }
}
