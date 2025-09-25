# File Storage Service

A REST API that lets users upload, list, rename, download, and delete files.  
It was built with **Java 17 + Spring Boot + MongoDB + S3-compatible storage (MinIO in dev)** and delivered as a **Docker image** with **CI building the image**.

> This repository focuses on correctness, clarity, and a maintainable architecture (ports & adapters / hexagonal) with pragmatic DDD boundaries.

---

## Quick start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- cURL or Postman

### Run the stack (MongoDB + MinIO)
```bash
docker compose -f stack_local_environment/docker-compose.yml up -d
```

### Build & run the app
```bash
./gradlew clean build
```

### Health check
```bash
curl -s {'{'}BASE{'}'}/actuator/health | jq .
# expect: status=UP
```

> Base URL used throughout the docs: `http://localhost:8080`.

---

## Using Postman (recommended)

Import the collection: **file-storage.postman_collection.json** (no environment needed).  
Each folder represents a **business requirement** and every request already sets headers and bodies.  
For **upload**, select a local file before sending.

- The **Upload** saves `fileId`, `linkId`, `filename` into **collection variables** for the next steps.
- To **re-run the upload**, change either the filename *or* the file content (uniqueness by owner applies to both).

---

## Business flows (API endpoints)

### 1) Upload (PUBLIC/PRIVATE), tags, MIME detection
- **POST** `/files` (multipart):
    - Header: `X-User-Id: <user>`
    - Parts:
        - `metadata` (JSON): `{"filename":"my.txt","visibility":"PUBLIC","tags":["Demo"]}`
        - `file`: the binary itself
- Rules:
    - Up to **5 tags**.
    - **Content type auto-detection** via Apache Tika when not provided by the client.
    - Uniqueness per owner by **filename** OR **content hash** (either match triggers 409).

### 2) List files
- **PUBLIC**: `GET /files/public`
- **OWNER**: `GET /files` (requires `X-User-Id`)
- Filters:
    - `tag` — **case-insensitive exact** match (`demo`, `Demo` → same)
    - `q` — case-insensitive **filename contains**
- Sorting: `sortBy=FILENAME|CREATED_AT|UPDATED_AT|CONTENT_TYPE|SIZE`, `sortDir=ASC|DESC`
- Pagination: `page` (>=0), `size` (1..100)

### 3) Rename (no reupload)
- **PATCH** `/files/{fileId}/name`
    - Header: `X-User-Id` (must be owner)
    - Body: `{"newFilename":"new-name.txt"}`

### 4) Download by link (PRIVATE and PUBLIC)
- **GET** `/d/{linkId}`
- The link is **unguessable** (UUID).
- Supports `ETag`, `Content-Length`, `Accept-Ranges`, and correct `Content-Type` headers.

### 5) Delete (owner only)
- **DELETE** `/files/{fileId}` with `X-User-Id` (must be owner).

---

## Observability

- **Spring Boot Actuator** exposed endpoints:
    - `/actuator/health` (liveness/readiness checks)
    - `/actuator/info`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`
- **HTTP request metrics** can be enabled via Micrometer if needed. For local scope, we kept the default set lean.  
  If Prometheus is required, expose `/actuator/prometheus` by adding to `application.yml`:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,env,beans,configprops,prometheus
  ```
- **Structured logs**: the API logs request mappings and Mongo queries at DEBUG to aid review; switch to INFO in production.

---

## Architecture (concise)

- **Hexagonal / Ports & Adapters** style:
    - **Domain** (entities, ports), **Application** (use cases), **Infrastructure** (Mongo repository, S3/MinIO adapter), **API** (controllers/DTOs).
- **DDD-lite**: the core model (`FileMetadata`, `FileStatus`, `Visibility`) stays free of framework noise.
- **SOLID**:
    - SRP: adapters do I/O only; service orchestrates rules; controllers translate HTTP ↔ use cases.
    - DIP: domain depends on `MetadataRepository` and `StoragePort` interfaces; concrete adapters are injected.
- **Trade-off noted**: API’s `ListQuery` is used across layers (kept for simplicity). With more time, we’d introduce an API→domain **query mapper**.

---

## Tests

- **Unit tests (service)**: upload rules (uniqueness by owner), rename ownership, content type handling.
- **Unit tests (controller)**: download headers (ETag, Content-Disposition, Accept-Ranges, etc.).
- **Edge tests**: delete invalidates link; pagination clamps.
- **Concurrency** simulation: covered in service tests & manual Postman guidance.

To run:
```bash
./gradlew test
open build/reports/tests/test/index.html
```

---

## CI & Docker

- **CI builds the Docker image** to prove reproducible packaging and to check the Dockerfile stays healthy.
- Why a Docker image in CI?
    - Ensures reviewers can pull/run the exact artifact that passed the pipeline.
    - Catches classpath/native packaging issues beyond the unit tests.
- Local image build:
```bash
docker build -t filestorage-app:local .
docker run --rm -p 8080:8080 --name filestorage-app   --env SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/filestorage   --env STORAGE_PROVIDER=s3   --env STORAGE_ENDPOINT=http://host.docker.internal:9000   --env STORAGE_ACCESSKEY=minioadmin   --env STORAGE_SECRETKEY=minioadmin   filestorage-app:local
```

---

## Full requirements checklist

**Functional**
- ✅ Upload a file (filename, visibility PUBLIC/PRIVATE, up to 5 tags, any size)
- ✅ Detect content type after upload if missing
- ✅ Return unguessable download link (works for PUBLIC and PRIVATE)
- ✅ Rename without reupload (owner-only)
- ✅ List PUBLIC files (filter by tag, case-insensitive exact; `q` in filename; sort; pagination)
- ✅ List OWNER files (same filters/sort/paging; requires `X-User-Id`)
- ✅ Delete only by owner
- ✅ Prevent duplicate uploads per owner by **filename** or **content hash**

**Non-functional**
- ✅ Dockerized app; CI builds the image
- ✅ Spring Boot; MongoDB as persistence
- ✅ RESTful API; no sessions/auth endpoints (assumes `X-User-Id`)
- ✅ Tests for controllers and services
- ✅ Gradle build
- ✅ README with API usage

---

## Points of Consideration (answers)

1. **Most interesting**: Designing a lean hexagonal core and making storage pluggable so MinIO can be swapped for real S3 without code changes.
2. **Most cumbersome**: Iterating on the business rules and end-to-end scripts to ensure every edge case (dup by name/hash, owner checks, link invalidation) was demonstrably covered.
3. **If I had more time**: Split API DTOs from domain (replace `ListQuery` with a domain `QuerySpec` + mapper); polish error taxonomy (business vs transport exceptions).
4. **Preparing for bigger loads**: Use real **S3** with multi-part uploads and lifecycle policies; move metadata writes to **Mongo transactions** (where applicable); enable async processing for virus scan/MIME/type; introduce **rate limits** and **backpressure**.
5. **Monitoring**: Keep Actuator; add Prometheus + Grafana dashboards (request rates, latencies, error ratios, S3/Mongo ops); structured logs with correlation IDs via filters.
6. **Copy file**: Store binary once (content-addressable key = hash) and create a **new metadata row** pointing to the same blob; on delete, decrement refcount—only GC blob when count=0; generate a **new linkId** for the copy.
7. **Improve persistence**: Move large payloads to S3 and keep **immutable** content references in Mongo; ensure unique indexes (`ownerId+filename`, `ownerId+contentHash`); add TTL/indexes where applicable for soft-deleted data.
8. **Versioning**: Append `version` to metadata keyed by `(ownerId, logicalName)`; each version points to a blob (often shared if same hash); `latest` pointer for quick read; list versions endpoint.
9. **Resume upload**: S3 multi-part upload API with upload IDs and byte-range tracking; on the API side, persist an **upload session** document (offsets, parts, expiry).

---

## Tips to re-run flows quickly

- Re-upload: change **filename** or the **file content** (both are checked for duplicates per owner).
- Tag filter: it’s **case-insensitive exact**; `tag=demo`, `Demo`, `dEmO` are equivalent.
- Use the **Postman collection**; after upload it will save `fileId` and `linkId` automatically.


