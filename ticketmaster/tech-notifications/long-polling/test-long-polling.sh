#!/bin/bash

USER_ID="user123"

# Start the process
echo "=== Starting process for $USER_ID ==="
curl -X POST http://localhost:8080/start/$USER_ID
echo -e "\n"

# Initial call without lastStatus (should return NOT_READY immediately)
echo "=== Poll 1: Initial request (no lastStatus) ==="
RESPONSE=$(curl -s http://localhost:8080/status/$USER_ID)
echo "$RESPONSE"
STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
echo "Status: $STATUS"
echo -e "\n"

# Long-poll with lastStatus=NOT_READY (waits for WAITING)
echo "=== Poll 2: Long-polling with lastStatus=NOT_READY (waits for WAITING) ==="
START_TIME=$(date +%s)
RESPONSE=$(curl -s "http://localhost:8080/status/$USER_ID?lastStatus=NOT_READY")
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "$RESPONSE"
STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
echo "Status: $STATUS (waited ${DURATION}s)"
echo -e "\n"

# Long-poll with lastStatus=WAITING (waits for READY)
echo "=== Poll 3: Long-polling with lastStatus=WAITING (waits for READY) ==="
START_TIME=$(date +%s)
RESPONSE=$(curl -s "http://localhost:8080/status/$USER_ID?lastStatus=WAITING")
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "$RESPONSE"
STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
echo "Status: $STATUS (waited ${DURATION}s)"
echo -e "\n"

echo "=== Test complete! ==="
echo "Expected: 3 requests instead of 7+ with regular polling"
echo "Poll 1: Immediate return with NOT_READY"
echo "Poll 2: Waited ~2s for WAITING"
echo "Poll 3: Waited ~3s for READY"
