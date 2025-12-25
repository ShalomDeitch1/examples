package com.example.ticketmaster.locking.dynamodb;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootApplication
public class DynamoDbLockingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DynamoDbLockingApplication.class, args);
    }

    @Bean
    DynamoDbClient dynamoDbClient(
            @Value("${app.dynamodb.endpoint}") String endpoint,
            @Value("${app.aws.region:us-east-1}") String region
    ) {
        // For local demos/tests (LocalStack). Real AWS uses DefaultCredentialsProvider.
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }
}
