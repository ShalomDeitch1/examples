#!/usr/bin/env bash
set -euo pipefail

baseUrl="${BASE_URL:-http://localhost:8080}"
userId="${USER_ID:-alice}"
callbackUrl="${CALLBACK_URL:-$baseUrl/user-app/webhooks/waiting-room}"

curl -s -X POST "$baseUrl/ticketmaster/register/$userId" \
  -H "Content-Type: application/json" \
  -d "{\"callbackUrl\":\"$callbackUrl\"}" \
  | cat

echo
curl -s -X POST "$baseUrl/ticketmaster/start/$userId" | cat

echo

echo "Polling inbox for up to ~10s..."
for i in {1..20}; do
  inbox=$(curl -s "$baseUrl/user-app/inbox/$userId")
  echo "$inbox" | cat
  if echo "$inbox" | grep -q "WAITING_ROOM_ACTIVE"; then
    echo "Received webhook event."
    exit 0
  fi
  sleep 0.5
done

echo "Timed out waiting for webhook. Is the app running on $baseUrl ?" >&2
exit 1
