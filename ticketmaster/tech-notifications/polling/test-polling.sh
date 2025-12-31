#!/bin/bash

USER_ID="user123"

# Start the process
echo "=== Starting process for $USER_ID ==="
curl -X POST http://localhost:8080/start/$USER_ID
echo -e "\n"

# Poll every 1 second for 7 seconds to catch all state transitions
echo "=== Polling for status changes ==="
for i in {1..7}; do
  echo "Poll $i (at ${i}s):"
  curl -X GET http://localhost:8080/status/$USER_ID
  echo -e "\n"
  
  # Exit if READY
  STATUS=$(curl -s http://localhost:8080/status/$USER_ID | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
  if [ "$STATUS" == "READY" ]; then
    echo "User is READY! Exiting poll loop."
    break
  fi
  
  sleep 1
done
