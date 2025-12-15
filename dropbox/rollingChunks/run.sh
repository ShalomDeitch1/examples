#!/usr/bin/env bash
set -euo pipefail

# run.sh - start LocalStack (if needed), ensure S3 bucket exists, then build and run the rollingChunks (Stage 4) Spring Boot app
# Usage: ./run.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DROPBOX_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Starting Stage 4 (rollingChunks) with LocalStack..."
echo "This will start LocalStack (if needed), create the bucket, then run the Spring Boot app."

"$DROPBOX_DIR/run-local.sh" "$SCRIPT_DIR"
