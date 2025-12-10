package com.example.dropbox.simplest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dropbox.simplest.model.FileMetadata;
import com.example.dropbox.simplest.model.FileRepository;
import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SimplestFlowTest {

        private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");
        private static final String BUCKET_NAME = "dropbox-simplest-bucket";

        @Container
        static LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
                        .withServices(LocalStackContainer.Service.S3);

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private FileRepository fileRepository;

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
                registry.add("aws.s3.endpoint",
                                () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
                registry.add("aws.region", localStack::getRegion);
                registry.add("aws.accessKeyId", localStack::getAccessKey);
                registry.add("aws.secretAccessKey", localStack::getSecretKey);
                registry.add("aws.s3.bucket", () -> BUCKET_NAME);
        }

        @BeforeAll
        static void setupS3() {
                S3Client s3Client = S3Client.builder()
                                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(localStack.getAccessKey(),
                                                                localStack.getSecretKey())))
                                .region(Region.of(localStack.getRegion()))
                                .build();

                s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        }

        @Test
        void testUploadAndDownloadFlow() throws Exception {
                // 1. Create a "large" file
                String fileContent = "This is a test file content designed to simulate a file upload.";
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-file.txt",
                                "text/plain",
                                fileContent.getBytes(StandardCharsets.UTF_8));

                // 2. Upload to Server
                MvcResult uploadResult = mockMvc.perform(multipart("/files/upload")
                                .file(file))
                                .andExpect(status().isOk())
                                .andReturn();

                // Extract ID from response
                String responseJson = uploadResult.getResponse().getContentAsString();
                Integer id = JsonPath.read(responseJson, "$.id");
                assertThat(id).isNotNull();

                // 3. Request Metadata + Presigned URL
                MvcResult getResult = mockMvc.perform(get("/files/" + id))
                                .andExpect(status().isOk())
                                .andReturn();

                String getResponseJson = getResult.getResponse().getContentAsString();
                String downloadUrl = JsonPath.read(getResponseJson, "$.downloadUrl");
                String fileName = JsonPath.read(getResponseJson, "$.metadata.fileName");

                assertThat(fileName).isEqualTo("test-file.txt");
                assertThat(downloadUrl)
                                .contains(localStack.getEndpointOverride(LocalStackContainer.Service.S3).getHost()); // basic
                                                                                                                     // check

                // 4. Download from S3 using Presigned URL
                // Since it's a presigned URL to LocalStack, we can try to open a stream to it.
                // NOTE: LocalStack presigned URLs might point to 'localhost' or internal Docker
                // IP.
                // Testcontainers maps ports. The URL generated by the app (running outside
                // container)
                // using the endpoint override SHOULD be accessible from the test runner.

                try (InputStream in = new URL(downloadUrl).openStream()) {
                        String downloadedContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        assertThat(downloadedContent).isEqualTo(fileContent);
                }
        }
}
