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
- `POST /api/files/{id}/complete`
  - Updates status to AVAILABLE (Simulates S3 event)
- `GET /api/files/{id}`
  - Returns: `{ "metadata": {...}, "downloadUrl": "http://..." }`
