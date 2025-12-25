package com.example.ticketmaster.locking.dynamodb;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Testcontainers
@SpringBootTest(classes = DynamoDbLockingApplication.class)
class DynamoDbLockingIntegrationTest {
    private static final String SEAT_TABLE = "seat_inventory";
    private static final String ORDER_TABLE = "orders";

    @Container
        static final GenericContainer<?> dynamodbLocal = new GenericContainer<>(
            DockerImageName.parse("amazon/dynamodb-local:2.5.2")
        ).withExposedPorts(8000);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint", () -> "http://" + dynamodbLocal.getHost() + ":" + dynamodbLocal.getMappedPort(8000));
        registry.add("app.aws.region", () -> "us-east-1");
    }

    @Autowired
    DynamoDbClient dynamoDbClient;

    @Autowired
    DynamoDbSeatLocker locker;

    @BeforeEach
    void resetTables() {
        deleteTableIfExists(SEAT_TABLE);
        deleteTableIfExists(ORDER_TABLE);

        createTable(SEAT_TABLE, "seat_id");
        createTable(ORDER_TABLE, "order_id");

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(SEAT_TABLE)
                .item(java.util.Map.of(
                        "seat_id", AttributeValue.builder().s("s1").build(),
                        "status", AttributeValue.builder().s("AVAILABLE").build()
                ))
                .build());
    }

    @Test
    void conditional_write_allows_only_one_winner() throws Exception {
        List<Boolean> results = runTwoThreadsTogether(
                () -> locker.reserveSeatConditionally(SEAT_TABLE, "s1", "o1"),
                () -> locker.reserveSeatConditionally(SEAT_TABLE, "s1", "o2")
        );

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    }

    @Test
    void transact_write_reserves_seat_and_creates_order_atomically() {
        boolean ok = locker.reserveSeatAndCreateOrderTransact(SEAT_TABLE, ORDER_TABLE, "s1", "o1");
        assertThat(ok).isTrue();

        boolean second = locker.reserveSeatAndCreateOrderTransact(SEAT_TABLE, ORDER_TABLE, "s1", "o2");
        assertThat(second).isFalse();
    }

    private void createTable(String tableName, String hashKey) {
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(hashKey)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(hashKey)
                        .keyType(KeyType.HASH)
                        .build())
                .build());

        // Wait for ACTIVE
        for (int i = 0; i < 50; i++) {
            String status = dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
                    .table().tableStatusAsString();
            if ("ACTIVE".equals(status)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Table did not become ACTIVE: " + tableName);
    }

    private void deleteTableIfExists(String tableName) {
        try {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
        } catch (ResourceNotFoundException ignored) {
        }
    }

    private static List<Boolean> runTwoThreadsTogether(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Boolean> wrappedA = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return a.call();
            };
            Callable<Boolean> wrappedB = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return b.call();
            };

            Future<Boolean> fa = executor.submit(wrappedA);
            Future<Boolean> fb = executor.submit(wrappedB);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return List.of(
                    fa.get(10, TimeUnit.SECONDS),
                    fb.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
