# Dropbox System Design

This set of projects is based on the helloInterview design question.
The goal is to implement different stages of a system design to understand the trade-offs and benefits of each approach.

## functional requirements
1. can store file
2. can retrieve files
3. other users can retrieve files (getting permission to access files is Out Of Scope)
4. can sync with changes done on local version
5. can sync with changes done by others
    - currently having multiple devices per user is Out Of Scope

## Nonfunctional Requirements
1. availability >> consistency (we do not care if it takes a few seconds to get an update, but we do not want an error getting something)
2. reasonable download/upload times (size dependant)
3. continue load if load gets stopped in the middle
4. big files

## Simulation Requirements
1. **LocalStack**: Used to simulate AWS Services (S3, etc.).
2. **AOP Delays**: Artificial delays introduced via AOP to simulate network load/processing time, allowing us to observe the impact of architectural decisions.

---

## Stage 1: Simplest Version (Naive Approach)

In this version, the client uploads the file directly to the application server. The server then uploads it to S3 and saves the metadata in the database.

### Design

```mermaid
flowchart TD
    Client[Client Device]
    AppServer[Application Server]
    DB[(Database)]
    S3[S3 Storage]

    Client -- .1. Upload File + Metadata. --> AppServer
    AppServer -- .2. Store Metadata. --> DB
    AppServer -- .3. Upload File. --> S3
```

```mermaid
sequenceDiagram
    participant Client
    participant AppServer
    participant DB
    participant S3

    Note over Client, S3: Upload Flow
    Client->>AppServer: POST /upload (File + Metadata)
    activate AppServer
    AppServer->>S3: PutObject(File)
    AppServer->>DB: Save Metadata (s3_path, size, name)
    AppServer-->>Client: 200 OK
    deactivate AppServer

    Note over Client, S3: Download Flow
    Client->>AppServer: GET /file/{id}
    activate AppServer
    AppServer->>DB: Get Metadata
    AppServer->>S3: Generate Presigned URL
    AppServer-->>Client: Metadata + Presigned URL
    deactivate AppServer
    Client->>S3: GET (Presigned URL)
```

### Problems
1.  **Double Bandwidth**: The file travels `Client -> Server` and then `Server -> S3`. This doubles the network IO on the server.
2.  **Blocking**: Large files block the application server threads/resources while being uploaded to S3.
3.  **Gateway Limits**: API Gateways often have strict payload size limits, preventing large file uploads.

---

## Stage 2: Direct S3 Upload (Presigned URLs)

To solve the double bandwidth and blocking issues, the client uploads files directly to S3 using a presigned URL.

### Design

```mermaid
flowchart TD
    Client[Client Device]
    AppServer[Application Server]
    DB[(Database)]
    S3[S3 Storage]

    Client -- .1. Init Upload. --> AppServer
    AppServer -- .2. Save Metadata (Pending). --> DB
    AppServer -- .3. Return Presigned URL. --> Client
    Client -- .4. Upload File. --> S3
    S3 -- .Async Notification. --> AppServer
    AppServer -- .5. Update Metadata (Available). --> DB
```

```mermaid
sequenceDiagram
    participant Client
    participant AppServer
    participant DB
    participant S3

    Note over Client, S3: Upload Flow
    Client->>AppServer: POST /upload/init (Metadata)
    activate AppServer
    AppServer->>DB: Save Metadata (Status: PENDING)
    AppServer->>S3: Generate Presigned PUT URL
    AppServer-->>Client: Presigned URL
    deactivate AppServer
    
    Client->>S3: PUT (Presigned URL, File Content)
    activate S3
    S3-->>Client: 200 OK
    S3->>AppServer: S3 Event Notification (ObjectCreated)
    deactivate S3
    
    activate AppServer
    AppServer->>DB: Update Metadata (Status: AVAILABLE)
    deactivate AppServer

    Note over Client, S3: Download Flow
    Client->>AppServer: GET /file/{id}
    activate AppServer
    AppServer->>DB: Get Metadata
    AppServer->>S3: Generate Presigned GET URL
    AppServer-->>Client: Metadata + Presigned URL
    deactivate AppServer
    Client->>S3: GET (Presigned URL)
```

### Solves
*   **Bandwidth**: Server only handles small metadata requests. Heavy lifting is done by S3.
*   **Scalability**: Server can handle many more concurrent users since it's not tied up with streaming bytes.

### Issues
*   **Complexity**: Requires handling async notifications from S3.
*   **Consistency**: What if the upload fails? The DB might have a stale "PENDING" record.

### Recommended Notification Flow (Production)

To make the notification path durable and production-ready we recommend S3 -> SNS -> SQS -> Consumer. This keeps a reliable queue for consumers and allows fan-out via SNS if multiple subscribers are needed.

```mermaid
flowchart TD
        S3[S3] -->|ObjectCreated| SNS[SNS Topic]
        SNS --> SQS[SQS Queue]
        SQS --> AppServer[Application Consumer]
```

Sequence (S3 -> SNS -> SQS):

```mermaid
sequenceDiagram
        participant S3
        participant SNS
        participant SQS
        participant App

        S3->>SNS: Publish ObjectCreated event
        SNS-->>SQS: Deliver Notification
        App->>SQS: Poll & Receive Message
        App->>App: Parse SNS envelope -> Extract S3 key
        App->>DB: Update metadata status (AVAILABLE)
```

Notification payloads:

 - Direct S3 event (inside `Message` for SNS envelope or as Records for direct SQS):

```json
{
    "Records":[
        {
            "eventVersion":"2.1",
            "eventSource":"aws:s3",
            "awsRegion":"us-east-1",
            "eventName":"ObjectCreated:Put",
            "s3":{
                "bucket":{"name":"test-bucket"},
                "object":{"key":"09ba5a75-c02d-4d57-b020-1d2016ce1d16"}
            }
        }
    ]
}
```

 - SNS envelope (what SQS receives when subscribed to SNS):

```json
{
    "Type":"Notification",
    "MessageId":"...",
    "TopicArn":"arn:aws:sns:...",
    "Message":"{ \"Records\":[ { \"s3\":{ \"object\":{ \"key\":\"...\" } } } ] }"
}
```

You can inspect messages in LocalStack using the AWS CLI with the `--endpoint-url` flag, for example:

```bash
aws --endpoint-url=http://localhost:4566 sns list-topics
aws --endpoint-url=http://localhost:4566 sqs list-queues
aws --endpoint-url=http://localhost:4566 sqs receive-message --queue-url http://localhost:4566/000000000000/your-queue
```

---

## Stage 3: Chunking & Fingerprinting

For large files and sync efficiency, we split files into chunks. We use fingerprinting (rolling hash or simple chunk hash) to detect duplicates.

### Design

*   Chunking and reassembly are **client-side**.
*   Client calculates a SHA-256 hash for each chunk.
*   Server stores **metadata only** (the "Recipe" / manifest: ordered list of (hash, length)).
*   Chunks are stored in S3 as **content-addressed objects**: `chunks/sha256/<hash>`.
*   Client calls `POST /api/files/init` with the manifest; server returns presigned PUT URLs for **missing** chunks.
*   Client uploads only missing chunks directly to S3.
*   Client calls `POST /api/files/{fileId}/versions/{versionId}/complete` to finalize.
    - If any expected chunks are still missing, the server returns `409` with presigned URLs for the missing chunk hashes.
*   Status flow: `PENDING` (new) / `UPDATING` (new version) → `AVAILABLE` when all expected chunk events are observed.

```mermaid
flowchart TD
    Client[Client Device]
    AppServer[Application Server]
    DB[(Database)]
    S3[S3 Storage]

    Client -- .1. Init (manifest). --> AppServer
    AppServer -- .2. Save metadata (PENDING/UPDATING). --> DB
    AppServer -- .3. Return missing hashes + presigned PUTs. --> Client
    Client -- .4. Upload missing chunks. --> S3
    S3 -- .Async notifications. --> AppServer
    Client -- .5. Complete version. --> AppServer
    AppServer -- .6. Mark AVAILABLE when all chunks seen. --> DB
```

```mermaid
sequenceDiagram
    participant Client
    participant AppServer
    participant DB
    participant S3

    Note over Client, S3: Upload Flow
    Client->>Client: Split -> chunks [A, B, C]
    Client->>Client: Hash -> [h(A), h(B), h(C)]
    Client->>AppServer: POST /api/files/init (fileName, contentType, parts=[(h, len)])
    AppServer->>DB: Save file + version (status PENDING/UPDATING)
    AppServer-->>Client: missingParts + presigned PUT URLs
    Client->>S3: PUT missing chunks (presigned)
    S3-->>AppServer: ObjectCreated notifications (via SNS/SQS)
    Client->>AppServer: POST /api/files/{fileId}/versions/{versionId}/complete
    AppServer->>DB: Mark AVAILABLE once all expected chunks are present
```

### Solves
*   **Network Efficiency**: Only changes (deltas) are transferred.
*   **Deduplication**: Identical chunks (across files/users) are stored only once.

### Notes
*   **AWS Constraints**: S3 Multipart upload has a 5MB minimum part size (except the last one). For this simulation (10 byte chunks), we might need to store chunks as individual small objects or ignore S3 multipart constraints by using putting individual objects.

### Download Flow
1.  Client requests file.
2.  Server retrieves "Recipe" (list of chunks) from DB.
3.  Server generates presigned URLs for each chunk (or checks if client already has them in a local cache - Out of Scope for now).
4.  Client downloads chunks in parallel and reassembles the file.

```mermaid
flowchart TB
  subgraph Stage3["Stage 3: Chunking — Download Flow"]
    Client["Client Device"]
    AppServer["Application Server"]
    DB[(Database)]
    S3["S3 Storage"]
    Reassemble["Reassemble File (Local)"]

    Client -->|"1. GET /file/{id}"| AppServer
    AppServer -->|"2. Get Recipe (chunk list)"| DB
    AppServer -->|"3. Generate Presigned URLs (chunks)"| S3
    AppServer -->|"4. Return Recipe + URLs"| Client
    Client -->|"5. Download Chunks (Parallel)"| S3
    Client -->|"6. Reassemble chunks locally"| Reassemble
  end
```

```mermaid
sequenceDiagram
    participant Client
    participant AppServer
    participant DB
    participant S3

    Client->>AppServer: GET /file/{id}
    activate AppServer
    AppServer->>DB: Get Recipe [h(A), h(B)]
    AppServer->>S3: Generate URLs for A, B
    AppServer-->>Client: Recipe + URLs
    deactivate AppServer
    
    par Download Chunks
        Client->>S3: GET chunk A
        Client->>S3: GET chunk B
    end
    
    Client->>Client: Reassemble File
```

---

## Stage 4: Notification Service (Async Updates)

To satisfy the requirement of "syncing with changes done by others" without inefficient blocking or high-latency long-polling.

### Design

**Client-Side Change Detection**:
The client runs a local file watcher. When a file is modified locally:
1.  Client recalculates hashes.
2.  Follows Stage 3 flow (Upload missing chunks, Finalize).

**Server-Side Notification**:
We use a Polling model for the simulation (Simulating a message queue buffer).
*   When a file is updated, the Server publishes an event to an SQS Queue unique to the client (or filtered).
*   Clients periodically **poll** their queue for updates.

```mermaid
flowchart TD
    ClientA["Client A (Uploader)"]
    ClientB["Client B (Receiver)"]
    AppServer[Application Server]
    SNS[SNS Topic]
    SQS[SQS Queue]

    ClientA -- .1. Upload File. --> AppServer
    AppServer -- .2. Publish Event. --> SNS
    SNS -- .3. Push Message. --> SQS
    ClientB -- .4. Poll Messages. --> SQS
    ClientB -- .5. Download Metadata. --> AppServer
```

```mermaid
sequenceDiagram
    participant ClientA
    participant ClientB
    participant AppServer
    participant SNS
    participant SQS_B

    Note over ClientA: Local File Watcher detects change
    ClientA->>AppServer: Complete Upload (Stage 3 flows)
    activate AppServer
    AppServer->>SNS: Publish Event ("File X Updated")
    deactivate AppServer
    
    SNS->>SQS_B: Push Notification
    
    Note over ClientB: Periodic Poll
    loop Every N Seconds
        ClientB->>SQS_B: ReceiveMessage
        activate SQS_B
        SQS_B-->>ClientB: [Message: "File X Updated"]
        deactivate SQS_B
    end
    
    ClientB->>AppServer: Get File X Metadata (Stage 3 Flow)
```

### Solves
*   **Real-time Updates**: Clients know about changes quickly.
*   **Efficiency**: Polling an empty SQS queue is cheaper/faster than querying a full database for "changes since X".
*   **Offline Tolerance**: SQS queues persist messages (default retention is 4 days). If a client is offline, the message waits in the queue until the client comes back online and polls.