#!/usr/bin/env bash
set -euo pipefail

# run.sh - start LocalStack (if needed), ensure S3 bucket exists, then build and run the chunkS3 Spring Boot app
# Usage: ./run.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
LOCALSTACK_CONTAINER_NAME=${LOCALSTACK_CONTAINER_NAME:-localstack}
LOCALSTACK_IMAGE=${LOCALSTACK_IMAGE:-localstack/localstack:latest}
LOCALSTACK_HOST=${LOCALSTACK_HOST:-localhost}
LOCALSTACK_PORT=${LOCALSTACK_PORT:-4566}
BUCKET_NAME=${BUCKET_NAME:-"dropbox-stage3"}
TEST_BUCKET_NAME=${TEST_BUCKET_NAME:-"dropbox-stage3-test"}

echo "Project dir: $PROJECT_DIR"
echo "LocalStack container name: $LOCALSTACK_CONTAINER_NAME"
echo "Bucket name: $BUCKET_NAME"

auto_start_localstack=true

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. please install docker and try again." >&2
  exit 1
fi

container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
if [ -z "$container_id" ]; then
  if [ "$auto_start_localstack" = true ]; then
    echo "Starting LocalStack container..."
    docker run -d --name $LOCALSTACK_CONTAINER_NAME \
      -p ${LOCALSTACK_PORT}:4566 \
      -e SERVICES=s3,sns,sqs,sts \
      $LOCALSTACK_IMAGE >/dev/null
    sleep 2
    container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
    if [ -z "$container_id" ]; then
      echo "Failed to start LocalStack container." >&2
      exit 1
    fi
  else
    echo "No LocalStack container found and auto-start disabled." >&2
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

# Use aws CLI when available, otherwise awslocal inside container
if command -v aws >/dev/null 2>&1; then
  echo "Creating S3 buckets $BUCKET_NAME and $TEST_BUCKET_NAME via aws CLI (endpoint override)..."
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api create-bucket --bucket "$BUCKET_NAME" --region us-east-1 || true
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api create-bucket --bucket "$TEST_BUCKET_NAME" --region us-east-1 || true

  echo "Configuring CORS for buckets..."
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api put-bucket-cors --bucket "$TEST_BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
else
  echo "aws CLI not found, using awslocal inside container to create buckets"
  docker exec $container_id awslocal s3 mb s3://$BUCKET_NAME || true
  docker exec $container_id awslocal s3 mb s3://$TEST_BUCKET_NAME || true
  echo "Configuring CORS for buckets via awslocal..."
  docker exec $container_id awslocal s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
  docker exec $container_id awslocal s3api put-bucket-cors --bucket "$TEST_BUCKET_NAME" --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }]
  }' || true
fi

echo "Building chunkS3 module (skip tests)..."
cd "$PROJECT_DIR"
mvn -DskipTests package

echo "Starting chunkS3 Spring Boot app..."
mvnd="$(command -v mvn)"
if [ -z "$mvnd" ]; then
  echo "mvn not found in PATH; please install Maven or run the app from your IDE." >&2
  exit 1
fi
mvn -Dspring-boot.run.profiles=local spring-boot:run
