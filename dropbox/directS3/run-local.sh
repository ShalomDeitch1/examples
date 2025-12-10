#!/usr/bin/env bash
set -euo pipefail

# run-local.sh - start LocalStack (if needed), ensure S3 bucket exists, then run the Spring Boot app
# Usage: ./run-local.sh

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)/.."
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCALSTACK_CONTAINER_NAME=${LOCALSTACK_CONTAINER_NAME:-localstack}
LOCALSTACK_IMAGE=${LOCALSTACK_IMAGE:-localstack/localstack:latest}
LOCALSTACK_HOST=${LOCALSTACK_HOST:-localhost}
LOCALSTACK_PORT=${LOCALSTACK_PORT:-4566}
BUCKET_NAME=${BUCKET_NAME:-"test-bucket"}

echo "Project dir: $PROJECT_DIR"
echo "LocalStack container name: $LOCALSTACK_CONTAINER_NAME"
echo "Bucket name: $BUCKET_NAME"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. please install docker and try again." >&2
  exit 1
fi

container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
if [ -z "$container_id" ]; then
  echo "Starting LocalStack container..."
  docker run -d --name $LOCALSTACK_CONTAINER_NAME -p ${LOCALSTACK_PORT}:4566 $LOCALSTACK_IMAGE >/dev/null
  sleep 2
  container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
  if [ -z "$container_id" ]; then
    echo "Failed to start LocalStack container." >&2
    exit 1
  fi
else
  echo "Found LocalStack container: $container_id"
fi

echo "Waiting for LocalStack health on http://$LOCALSTACK_HOST:$LOCALSTACK_PORT ..."
RETRIES=30
i=0
while [ $i -lt $RETRIES ]; do
  if curl -sS "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT/health" >/dev/null 2>&1; then
    echo "LocalStack is healthy"
    break
  fi
  echo -n "."
  i=$((i+1))
  sleep 1
done
if [ $i -ge $RETRIES ]; then
  echo "LocalStack health check failed; attempting container-inside check..."
  if docker exec $container_id awslocal --version >/dev/null 2>&1; then
    echo "awslocal available inside container; proceeding"
  else
    echo "LocalStack not responding on $LOCALSTACK_HOST:$LOCALSTACK_PORT and awslocal not available in container. Exiting." >&2
    exit 1
  fi
fi

if command -v aws >/dev/null 2>&1; then
  echo "Creating S3 bucket $BUCKET_NAME via aws CLI (endpoint override)..."
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api create-bucket --bucket "$BUCKET_NAME" --region us-east-1 || true
else
  echo "aws CLI not found, using awslocal inside container to create bucket $BUCKET_NAME"
  docker exec $container_id awslocal s3 mb s3://$BUCKET_NAME || true
fi

echo "Building and running the Spring Boot app..."
cd "$PROJECT_DIR"
mvn -DskipTests package

# Run the app (foreground)
mvn spring-boot:run
