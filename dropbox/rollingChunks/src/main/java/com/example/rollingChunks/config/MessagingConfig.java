package com.example.rollingChunks.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class MessagingConfig {
    // In LocalStack (docker-compose) all services share :4566, but in Testcontainers each service can have a different mapped port.
    @Value("${aws.sns.endpoint:${aws.s3.endpoint}}")
    private URI snsEndpoint;

    @Value("${aws.sqs.endpoint:${aws.s3.endpoint}}")
    private URI sqsEndpoint;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .endpointOverride(snsEndpoint)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(sqsEndpoint)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(sqsEndpoint)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }
}
