#!/bin/bash

USER_ID="user123"

echo "=== Starting process for $USER_ID ==="
curl -X POST http://localhost:8080/start/$USER_ID
echo -e "\n"

echo "=== Subscribing to SSE stream for $USER_ID ==="
echo "Expected: One continuous connection with 3 events (NOT_READY, WAITING, READY)"
echo "Connection will close automatically after READY is received."
echo ""

# Subscribe to SSE stream
# Note: curl will keep connection open and display events as they arrive
curl -N http://localhost:8080/subscribe/$USER_ID

echo -e "\n=== Stream completed! ==="
echo "You should have seen:"
echo "1. event: status-update + data: {\"userId\":\"user123\",\"status\":\"NOT_READY\",...}"
echo "2. event: status-update + data: {\"userId\":\"user123\",\"status\":\"WAITING\",...} (after ~2s)"
echo "3. event: status-update + data: {\"userId\":\"user123\",\"status\":\"READY\",...} (after ~3s more)"
