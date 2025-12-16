#!/usr/bin/env bash
set -euo pipefail

# run.sh - start LocalStack (if needed), ensure S3 bucket exists, then build and run the multipartUpload Spring Boot app
# Usage: ./run.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DROPBOX_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Starting multipartUpload (Option A) with LocalStack..."
"$DROPBOX_DIR/run-local.sh" "$SCRIPT_DIR"
