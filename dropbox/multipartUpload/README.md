# Multipart Upload (Option A)

This module is a minimal reference implementation of **direct-to-S3 multipart upload** using:

- `CreateMultipartUpload` (server)
- `UploadPart` via **presigned URLs** (client/browser)
- `CompleteMultipartUpload` (server)

It is intentionally small and educational, designed as a “copy this architecture” starting point.

## When to use this (Option A)

Use Option A when:

- You want the **simplest production upload architecture** for large files.
- You want the client to send bytes **directly to S3** (no app-server streaming).
- You don’t need cross-file/version **deduplication**.

This is the most common pattern for “upload a file to S3 from a browser”.

## When not to use this

If you need Dropbox-style sync efficiency (small edits upload only a few chunks), use **Option B** (content-addressed chunk store + manifest), implemented in `rollingChunks`.

## Flow

```mermaid
flowchart TD
    Client[Browser / Client]
    App[App Server]
    S3[(S3)]

    Client -->|1. init metadata| App
    App -->|2. CreateMultipartUpload| S3
    App -->|3. return uploadId + key + partSize| Client

    loopParts[Upload parts] -->|4. presign UploadPart URLs| App
    App -->|5. presigned URL| Client
    Client -->|6. PUT part N to S3| S3

    Client -->|7. complete parts + ETags| App
    App -->|8. CompleteMultipartUpload| S3
    App -->|9. return download URL| Client
```

```mermaid
sequenceDiagram
    participant Client
    participant App
    participant S3

    Client->>App: POST /api/multipart/init
    App->>S3: CreateMultipartUpload
    App-->>Client: uploadId, objectKey, partSizeBytes

    loop For each part
        Client->>App: POST /api/multipart/presign (uploadId, objectKey, partNumber)
        App-->>Client: presigned UploadPart URL
        Client->>S3: PUT part bytes to presigned URL
        S3-->>Client: 200 OK + ETag
    end

    Client->>App: POST /api/multipart/complete (uploadId, objectKey, parts[])
    App->>S3: CompleteMultipartUpload
    App-->>Client: presigned GET URL
```

## Running locally (UI)

This repo has a shared runner that starts LocalStack and creates the bucket.

From WSL:

- `cd /mnt/c/projects/learn/examples/dropbox/multipartUpload && ./run-local.sh`

UI:

- `http://localhost:8080`

## Notes / Production considerations

- **Part size**: real AWS S3 requires each part to be at least **5 MiB**, except the last part.
  - For local demos we allow smaller part sizes.
- **Abandoned uploads cleanup**: you must implement a TTL-based cleanup that calls `AbortMultipartUpload`.
- **ETags are opaque**: treat them as identifiers required by `CompleteMultipartUpload`.

