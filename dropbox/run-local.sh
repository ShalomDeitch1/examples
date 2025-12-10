#!/usr/bin/env bash
set -euo pipefail

# run-local.sh - start LocalStack (if needed), ensure S3 bucket exists, then run the Spring Boot app
# Usage: ./run-local.sh [project-dir]

PROJECT_DIR=${1:-"$(pwd)/directS3"}
LOCALSTACK_CONTAINER_NAME=${LOCALSTACK_CONTAINER_NAME:-localstack}
LOCALSTACK_IMAGE=${LOCALSTACK_IMAGE:-localstack/localstack:latest}
LOCALSTACK_HOST=${LOCALSTACK_HOST:-localhost}
LOCALSTACK_PORT=${LOCALSTACK_PORT:-4566}
BUCKET_NAME=${BUCKET_NAME:-$(grep "^aws.s3.bucket" -H "$PROJECT_DIR/src/main/resources/application.properties" | cut -d'=' -f2 | tr -d '[:space:]')}

if [ -z "$BUCKET_NAME" ]; then
  BUCKET_NAME="test-bucket"
fi

echo "Project dir: $PROJECT_DIR"
echo "LocalStack container name: $LOCALSTACK_CONTAINER_NAME"
echo "Bucket name: $BUCKET_NAME"

# Check Docker
if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. please install docker and try again." >&2
  exit 1
fi

# Start LocalStack container if not running
container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
if [ -z "$container_id" ]; then
  echo "Starting LocalStack container..."
  docker run -d --name $LOCALSTACK_CONTAINER_NAME -p ${LOCALSTACK_PORT}:4566 $LOCALSTACK_IMAGE >/dev/null
  # wait a moment for container to start
  sleep 2
  container_id=$(docker ps --filter "name=$LOCALSTACK_CONTAINER_NAME" --format "{{.ID}}")
  if [ -z "$container_id" ]; then
    echo "Failed to start LocalStack container." >&2
    exit 1
  fi
else
  echo "Found LocalStack container: $container_id"
fi

# Wait for LocalStack health
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
  # Try awslocal inside container to see if service is available
  if docker exec $container_id awslocal --version >/dev/null 2>&1; then
    echo "awslocal available inside container; proceeding"
  else
    echo "LocalStack not responding on $LOCALSTACK_HOST:$LOCALSTACK_PORT and awslocal not available in container. Exiting." >&2
    exit 1
  fi
fi

# Create bucket using aws CLI if available, otherwise use awslocal inside container
if command -v aws >/dev/null 2>&1; then
  echo "Creating S3 bucket $BUCKET_NAME via aws CLI (endpoint override)..."
  aws --endpoint-url "http://$LOCALSTACK_HOST:$LOCALSTACK_PORT" s3api create-bucket --bucket "$BUCKET_NAME" --region us-east-1 || true
else
  echo "aws CLI not found, using awslocal inside container to create bucket $BUCKET_NAME"
  docker exec $container_id awslocal s3 mb s3://$BUCKET_NAME || true
fi

# Build and run app
echo "Building and running the Spring Boot app..."
cd "$PROJECT_DIR"
# Build first to ensure dependencies are available
mvn -DskipTests package

# Run the app (foreground)
mvn spring-boot:run
