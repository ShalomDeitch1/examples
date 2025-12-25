package com.example.ticketmaster.locking.dynamodb;


import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Component
public class DynamoDbSeatLocker {
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbSeatLocker(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Returns true if this call successfully reserved the seat.
     * Uses a conditional update (no blocking; DynamoDB enforces "one winner").
     */
    public boolean reserveSeatConditionally(String tableName, String seatId, String orderId) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("seat_id", builder().s(seatId).build()))
                    .updateExpression("SET #s = :reserved, order_id = :order")
                    .conditionExpression("#s = :available")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(
                            ":available", builder().s("AVAILABLE").build(),
                            ":reserved", builder().s("RESERVED").build(),
                            ":order", builder().s(orderId).build()
                    ))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    /**
     * Returns true if this call successfully reserved the seat and created the order atomically.
     * Demonstrates TransactWriteItems across two items.
     */
    public boolean reserveSeatAndCreateOrderTransact(String seatTable, String orderTable, String seatId, String orderId) {
        Objects.requireNonNull(seatTable);
        Objects.requireNonNull(orderTable);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(orderId);

        try {
            final TransactWriteItem reserveSeat = TransactWriteItem.builder()
                    .update(u -> u.tableName(seatTable)
                            .key(Map.of("seat_id", builder().s(seatId).build()))
                            .updateExpression("SET #s = :reserved, order_id = :order")
                            .conditionExpression("#s = :available")
                            .expressionAttributeNames(Map.of("#s", "status"))
                            .expressionAttributeValues(Map.of(
                                    ":available", builder().s("AVAILABLE").build(),
                                    ":reserved", builder().s("RESERVED").build(),
                                    ":order", builder().s(orderId).build()
                            )))
                    .build();

            Map<String, AttributeValue> orderItem = Map.of(
                    "order_id", builder().s(orderId).build(),
                    "seat_id", builder().s(seatId).build()
            );

            TransactWriteItem createOrder = TransactWriteItem.builder()
                    .put(Put.builder()
                            .tableName(orderTable)
                            .item(orderItem)
                            .conditionExpression("attribute_not_exists(order_id)")
                            .build())
                    .build();

            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                    .transactItems(reserveSeat, createOrder)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException | TransactionCanceledException e) {
            return false;
    }
  }
}
