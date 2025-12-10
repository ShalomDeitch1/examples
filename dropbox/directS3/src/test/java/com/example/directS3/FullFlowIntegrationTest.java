package com.example.directS3;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.repository.FileMetadataRepository;
import com.example.directS3.service.FileService;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

  @Container
  static LocalStackContainer localStack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest"));

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private FileMetadataRepository repository;

  // We need a separate RestTemplate for S3 Presigned URLs
  private final RestTemplate externalRestTemplate = new RestTemplate();

  @DynamicPropertySource
  static void overrideConfiguration(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.endpoint",
        () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    registry.add("aws.s3.region", () -> localStack.getRegion());
    registry.add("aws.accessKeyId", () -> localStack.getAccessKey());
    registry.add("aws.secretAccessKey", () -> localStack.getSecretKey());
    registry.add("aws.s3.bucket", () -> "test-bucket");
  }

  @BeforeAll
  static void setupS3() {
    S3Client s3 = S3Client.builder()
        .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
        .credentialsProvider(StaticCredentialsProvider
            .create(AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
        .region(Region.of(localStack.getRegion()))
        .build();

    try {
      s3.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build());
    } catch (Exception e) {
      // ignore
    }
  }

  @Test
  @Order(1)
  void testDirectS3UploadFlow() {
    String fileName = "test-direct.txt";
    String content = "Hello World";

    // 1. Init Upload
    Map<String, Object> initRequest = Map.of("fileName", fileName, "size", (long)content.length());
    ResponseEntity<FileService.UploadResponse> initResponse = restTemplate.postForEntity(
        "/api/files/upload/init", initRequest, FileService.UploadResponse.class);

    assertThat(initResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    FileService.UploadResponse body = initResponse.getBody();
    assertThat(body).isNotNull();
    String uploadUrl = body.uploadUrl();
    UUID fileId = body.fileId();

    // 2. Client Uploads to S3 (using the presigned URL)
    HttpHeaders headers = new HttpHeaders();
    // S3 Presigned URLs usually don't strictly require Content-Type unless signed
    // with it, but good practice.
    HttpEntity<byte[]> entity = new HttpEntity<>(content.getBytes(StandardCharsets.UTF_8), headers);
    externalRestTemplate.put(java.net.URI.create(uploadUrl), entity);

    // Verify Pending State
    ResponseEntity<String> failCheck = restTemplate.getForEntity("/api/files/" + fileId, String.class);
    assertThat(failCheck.getStatusCode().is5xxServerError()).isTrue();

    // 3. Simulate S3 Event Notification (Webhook)
    // Retrieve S3 Key from DB to construct the event
    FileMetadata savedMetadata = repository.findById(fileId).orElseThrow();
    String s3Key = savedMetadata.getS3Key();

    String s3EventJson = """
        {
          "Records": [
            {
              "s3": {
                "object": {
                  "key": "%s"
                }
              }
            }
          ]
        }
        """.formatted(s3Key);

    HttpHeaders webhookHeaders = new HttpHeaders();
    webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
    restTemplate.postForEntity("/api/hooks/s3", new HttpEntity<>(s3EventJson, webhookHeaders), Void.class);

    // 4. Verify Available
    ResponseEntity<FileService.DownloadResponse> downloadResp = restTemplate.getForEntity("/api/files/" + fileId,
        FileService.DownloadResponse.class);
    assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(downloadResp.getBody().downloadUrl()).contains("test-bucket");
  }

  @Test
  @Order(2)
  void testPerformanceComparison() {
    // Use a smaller file for functional passing in CI/Dev env, but logic stands.
    byte[] largeContent = new byte[1024 * 1024]; // 1MB

    // --- Simple Upload ---
    long startSimple = System.nanoTime();
    HttpHeaders parts = new HttpHeaders();
    parts.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new ByteArrayResource(largeContent) {
      @Override
      public String getFilename() {
        return "large-simple.dat";
      }
    });

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, parts);
    restTemplate.postForEntity("/api/simple/files/upload", requestEntity, Void.class);
    long durationSimple = System.nanoTime() - startSimple;

    System.out.println("Simple Upload took: " + TimeUnit.NANOSECONDS.toMillis(durationSimple) + " ms");

    // --- Direct Upload ---
    long startDirect = System.nanoTime();

    // 1. Init
    Map<String, Object> initRequest = Map.of("fileName", "large-direct.dat", "size", (long) largeContent.length);
    FileService.UploadResponse initResp = restTemplate.postForObject("/api/files/upload/init", initRequest,
        FileService.UploadResponse.class);

    // 2. Put
    externalRestTemplate.put(java.net.URI.create(initResp.uploadUrl()), new HttpEntity<>(largeContent));

    // 3. Complete (via webhook this time for fairness? or manual? Manual is
    // distinct http call)
    // Let's use manual complete endpoint for speed in test, assuming notification
    // is async decoupled
    restTemplate.postForObject("/api/files/" + initResp.fileId() + "/complete", null, Void.class);

    long durationDirect = System.nanoTime() - startDirect;
    System.out.println("Direct Upload took: " + TimeUnit.NANOSECONDS.toMillis(durationDirect) + " ms");
  }
}
