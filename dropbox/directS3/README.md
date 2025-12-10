# Dropbox Stage 2: Direct S3 Upload

## Overview
This project implements Stage 2 of the Dropbox system design, featuring direct file uploads to S3 using Presigned URLs to offload bandwidth from the application server.

## Architecture
- **Client**: Requests upload URL, uploads to S3, notifies completion (or S3 notifies server).
- **App Server**: Generates Presigned URLs, manages metadata.
- **S3 (LocalStack)**: Stores files.
- **Database (H2)**: Stores file metadata (status, size, etc.).

## Prerequisites
- Java 21
- Maven
- Docker (for LocalStack)

## Running
1. Start LocalStack (if not using Testcontainers for dev):
   ```bash
   docker run --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Endpoints
- `POST /api/files/upload/init`
  - Body: `{ "fileName": "test.txt", "size": 123 }`
  - Returns: `{ "fileId": "...", "uploadUrl": "http://..." }`
- `PUT [uploadUrl]`
  - Body: File content
- Note: The server relies on S3 -> SNS -> SQS notifications to mark uploads AVAILABLE. There is no manual `/api/files/{id}/complete` endpoint.

Reconciliation behavior: if a client uploads directly to S3 without first calling the `upload/init` endpoint, the application will reconcile on the S3 notification. When the SQS listener processes the S3 event it will create a minimal metadata record for the object (using the S3 key) and mark it AVAILABLE so the file becomes visible in the application. This keeps the system tolerant of clients that skip the init/complete sequence but does not capture original filename/size unless the client performed `init` beforehand.
- `GET /api/files/{id}`
  - Returns: `{ "metadata": {...}, "downloadUrl": "http://..." }`
