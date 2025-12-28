#!/usr/bin/env bash
set -euo pipefail

# Delete duplicate core classes from kafka module
rm -f kafka/src/main/java/com/example/ticketmaster/waitingroom/kafka/WaitingRoomSession.java
rm -f kafka/src/main/java/com/example/ticketmaster/waitingroom/kafka/WaitingRoomSessionStatus.java
rm -f kafka/src/main/java/com/example/ticketmaster/waitingroom/kafka/WaitingRoomStore.java
rm -f kafka/src/main/java/com/example/ticketmaster/waitingroom/kafka/GrantHistory.java
rm -f kafka/src/main/java/com/example/ticketmaster/waitingroom/kafka/WaitingRoomCapacityProperties.java
rm -f kafka/src/test/java/com/example/ticketmaster/waitingroom/kafka/WaitingRoomStoreTest.java

# Delete duplicate core classes from rabbitmq module
rm -f rabbitmq/src/main/java/com/example/ticketmaster/waitingroom/rabbitmq/WaitingRoomSession.java
rm -f rabbitmq/src/main/java/com/example/ticketmaster/waitingroom/rabbitmq/WaitingRoomSessionStatus.java
rm -f rabbitmq/src/main/java/com/example/ticketmaster/waitingroom/rabbitmq/WaitingRoomStore.java
rm -f rabbitmq/src/main/java/com/example/ticketmaster/waitingroom/rabbitmq/GrantHistory.java
rm -f rabbitmq/src/main/java/com/example/ticketmaster/waitingroom/rabbitmq/WaitingRoomCapacityProperties.java
rm -f rabbitmq/src/test/java/com/example/ticketmaster/waitingroom/rabbitmq/WaitingRoomStoreTest.java

# Delete duplicate core classes from redis-streams module
rm -f redis-streams/src/main/java/com/example/ticketmaster/waitingroom/redisstreams/WaitingRoomSession.java
rm -f redis-streams/src/main/java/com/example/ticketmaster/waitingroom/redisstreams/WaitingRoomSessionStatus.java
rm -f redis-streams/src/main/java/com/example/ticketmaster/waitingroom/redisstreams/WaitingRoomStore.java
rm -f redis-streams/src/main/java/com/example/ticketmaster/waitingroom/redisstreams/GrantHistory.java
rm -f redis-streams/src/main/java/com/example/ticketmaster/waitingroom/redisstreams/WaitingRoomCapacityProperties.java
rm -f redis-streams/src/test/java/com/example/ticketmaster/waitingroom/redisstreams/WaitingRoomStoreTest.java

# Delete duplicate core classes from sqs module
rm -f sqs/src/main/java/com/example/ticketmaster/waitingroom/sqs/WaitingRoomSession.java
rm -f sqs/src/main/java/com/example/ticketmaster/waitingroom/sqs/WaitingRoomSessionStatus.java
rm -f sqs/src/main/java/com/example/ticketmaster/waitingroom/sqs/WaitingRoomStore.java
rm -f sqs/src/main/java/com/example/ticketmaster/waitingroom/sqs/GrantHistory.java
rm -f sqs/src/main/java/com/example/ticketmaster/waitingroom/sqs/WaitingRoomCapacityProperties.java
rm -f sqs/src/test/java/com/example/ticketmaster/waitingroom/sqs/WaitingRoomStoreTest.java

echo "Cleanup complete. Deleted duplicate core classes from all modules."
