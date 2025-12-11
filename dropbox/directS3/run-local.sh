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
BUCKET_NAME=${BUCKET_NAME:-"dropbox-stage2"}

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
  docker run -d --name $LOCALSTACK_CONTAINER_NAME \
    -p ${LOCALSTACK_PORT}:4566 \
    -e SERVICES=s3,sns,sqs,dynamodb,cloudformation,sts,cloudwatch \
    $LOCALSTACK_IMAGE >/dev/null
  sleep 2
  container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
  if [ -z "$container_id" ]; then
    echo "Failed to start LocalStack container." >&2
    exit 1
  fi
else
  echo "Found LocalStack container: $container_id"
  # Verify port 4566 is accessible
  port_check=$(docker port $container_id 4566 2>/dev/null || echo "")
  if [ -z "$port_check" ]; then
    echo "WARNING: Existing LocalStack container doesn't expose port 4566."
    echo "Stopping and restarting with correct configuration..."
    docker stop $container_id >/dev/null 2>&1
    docker rm $container_id >/dev/null 2>&1
    docker run -d --name $LOCALSTACK_CONTAINER_NAME \
      -p ${LOCALSTACK_PORT}:4566 \
      -e SERVICES=s3,sns,sqs,dynamodb,cloudformation,sts,cloudwatch \
      $LOCALSTACK_IMAGE >/dev/null
    sleep 2
    container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
  fi
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
  
  echo "Configuring CORS for bucket $BUCKET_NAME..."
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
else
  echo "aws CLI not found, using awslocal inside container to create bucket $BUCKET_NAME"
  docker exec $container_id awslocal s3 mb s3://$BUCKET_NAME || true
  
  echo "Configuring CORS for bucket $BUCKET_NAME..."
  docker exec $container_id awslocal s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
fi

echo "Building and running the Spring Boot app..."
cd "$PROJECT_DIR"
mvn -DskipTests package

# Run the app (foreground)
mvn spring-boot:run
