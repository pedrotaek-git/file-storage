# File Storage Service

A minimal file storage API built with **Java 17** and **Spring Boot 3**, using **MongoDB** for metadata and **S3-compatible storage (MinIO)** for file blobs. The service exposes endpoints to **upload**, **list**, **rename**, **download** (via public links) and **delete** files, respecting **visibility** (PUBLIC/PRIVATE), **tags**, **hash-based dedup rules** and **owner-based authorization**.

> This repository is a compact, production-lean MVP that demonstrates clean layering (Ports & Adapters / “Hexagonal”), practical trade‑offs, and thorough verification with cURL and tests.

---

## Table of Contents
- [Quick Start](#quick-start)
- [Requirements](#requirements)
- [Tech Stack & Architecture](#tech-stack--architecture)
- [Business Rules (overview)](#business-rules-overview)
- [How to Validate the API](#how-to-validate-the-api)
    - [Using Postman (recommended)](#using-postman-recommended)
    - [Using cURL (one-liners)](#using-curl-one-liners)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Operational Notes](#operational-notes)
- [What I would improve with more time](#what-i-would-improve-with-more-time)
- [Short Q&A (reflection)](#short-qa-reflection)

---

## Quick Start

> Assumes you already have **Java 17+**, **Docker** and **Docker Compose** installed.

1. **Start dependencies (MongoDB + MinIO):**
   ```bash
   docker compose -f stack_local_environment/docker-compose.yml up -d
   ```

2. **Run the application (choose one):**
    - Gradle:
      ```bash
      ./gradlew clean bootRun
      ```
    - Jar (after building):
      ```bash
      ./gradlew clean build
      java -jar build/libs/file-storage.jar
      ```

3. **Healthcheck:**
   ```bash
   curl -sS http://localhost:8080/actuator/health | jq -r .status
   # should print: UP
   ```

> MinIO Console is at http://localhost:9001 (user/pass: `minioadmin` / `minioadmin`). The app auto-creates the bucket configured in `application.yml` on startup.

---

## Requirements

- **Java**: 17+
- **Docker & Docker Compose**
- **(Optional) Postman**: Import the collection at `docs/postman/collections/File Storage.postman_collection.json`.

---

## Tech Stack & Architecture

- **Language/Framework**: Java 17, Spring Boot 3.4
- **Metadata Store**: MongoDB 7.x
- **Blob Store**: S3-compatible (MinIO)
- **Build**: Gradle (Kotlin DSL)
- **Observability**: Spring Boot Actuator (health/config/beans)
- **Architecture**: Ports & Adapters (“Hexagonal”)
    - **Domain** defines **ports** (`StoragePort`, `MetadataRepository`) and core model (`FileMetadata`, `Visibility`, `FileStatus`).
    - **Infrastructure** provides adapters: **S3/MinIO**, **Mongo**, (and a **Local FS** adapter for dev).
    - **Application** orchestrates business rules; **API** exposes REST controllers.
- **Why this approach?**
    - Easy to swap storage/repo implementations.
    - Pure domain logic stays testable and independent from frameworks.
    - Minimal surface yet production-friendly configuration (indices, bucket init).

> Note: This service is **not** event-driven and does **not** use messaging or MySQL. It intentionally focuses on the simplest reliable stack to satisfy the core requirements.

---

## Business Rules (overview)

- **Upload** (`/files`, multipart):
    - Requires header **`X-User-Id`**.
    - Visibility: **PUBLIC** or **PRIVATE**.
    - Tags: optional list of strings.
    - Computes **content hash** and stores alongside metadata.
    - **Conflicts (409)** when:
        - same **owner + filename** already exists, or
        - same **owner + content hash** already exists.
    - Different owners can upload the same content.
- **List public** (`/files/public`):
    - Filter by **tag** (case-insensitive); pagination + sorting.
- **List by owner** (`/files` with `X-User-Id`):
    - Same filters/pagination/sorting scoped by owner.
- **Rename** (`PATCH /files/{{id}}/name`):
    - Only the **owner** can rename (403 otherwise).
- **Download by link** (`GET /d/{{linkId}}`):
    - Returns the blob with headers: `ETag` (hash), `Content-Disposition`, `Accept-Ranges`, `Content-Type`, `Content-Length`.
- **Delete** (`DELETE /files/{{id}}` with owner header):
    - Removes metadata and **invalidates the link** (subsequent downloads return 404).

---

## How to Validate the API

### Using Postman (recommended)

1. Open Postman and **Import** the collection:
    - `docs/postman/collections/File Storage.postman_collection.json`

2. Set a collection variable:
    - `baseUrl = http://localhost:8080`

3. Run requests in order:
    - **Health / Actuator**
    - **Upload – PUBLIC (Demo tag)** → copy `id`, `linkId`, `contentHash`
    - **List public by tag (demo)** → should return the file
    - **List by owner (demo)** → header `X-User-Id: u1`
    - **Rename (owner ok)** → PATCH `/files/{id}/name`
    - **Download by link** → check headers; the `ETag` equals the `contentHash`
    - **Upload – PRIVATE (secret tag)** → should NOT show in public listing
    - **Negative upload (no user header)** → 400
    - **Duplicate name/content** → 409 rules
    - **Delete** → then **Download by link** should be 404

### Using cURL (one-liners)

> The collection contains the same flows. Here are two quick starters:

- **Upload PUBLIC with Demo tag**
  ```bash
  TS=$(date +%s%N); echo "demo-$TS" > /tmp/demo-$TS.txt
  curl -sS -X POST {{baseUrl}}/files -H "X-User-Id: u1"     -F "metadata={{"filename":"demo-$TS.txt","visibility":"PUBLIC","tags":["Demo"]}};type=application/json"     -F "file=@/tmp/demo-$TS.txt;type=text/plain" | jq .
  ```

- **List public by tag (case-insensitive)**
  ```bash
  curl -sS "{{baseUrl}}/files/public?tag=demo" | jq '.[].filename'
  ```

> For a complete shell walkthrough, see the Postman requests which mirror the end-to-end scenarios.

---

## Project Structure

```
src
├── main
│   ├── java/com/digitalarkcorp/filestorage
│   │   ├── api            # REST controllers + DTOs + error handling
│   │   ├── application    # orchestration/use cases
│   │   ├── domain         # core model + ports (no Spring)
│   │   └── infrastructure # adapters: s3/minio, mongo, fs + config
│   └── resources
│       └── application.yml
└── test
    └── java/com/digitalarkcorp/filestorage
        ├── DefaultFileServiceUnitTest           # core business rules
        ├── DeleteInvalidatesLinkTest            # link invalidation behavior
        ├── api/DownloadControllerHeadersTest    # HTTP headers contract
        └── FileStorageApplicationTests          # fast smoke
```

---

## Testing

- **Build & unit tests**
  ```bash
  ./gradlew clean build
  # reports:
  # - unit: build/reports/tests/test/index.html
  ```

- **Coverage (optional)**
  If you enable the JaCoCo plugin in `build.gradle.kts`:
  ```kotlin
  plugins { jacoco }
  tasks.test { finalizedBy(tasks.jacocoTestReport) }
  jacocoTestReport { reports { xml.required.set(true); html.required.set(true) } }
  ```
  Then run:
  ```bash
  ./gradlew test jacocoTestReport
  # report: build/reports/jacoco/test/html/index.html
  ```

- **Why these tests?**
    - Focus on **what matters**: business rules (duplicates, ownership, visibility) and **HTTP contract** for downloads.
    - Fast, isolated (no need to boot infra) thanks to **ports/adapters** and fakes.

---

## Operational Notes

- **Actuator**
    - `GET /actuator/health` → `UP`
    - `GET /actuator/beans` → confirms which storage adapter is active (S3StorageAdapter vs Local)
    - `GET /actuator/configprops` → storage configuration overview
- **Mongo Indexes**
    - Created on startup (unique by `(ownerId, filename)` and `(ownerId, contentHash)`, plus secondary indexes).
- **MinIO**
    - Bucket auto-created if missing; credentials are configured in `application.yml` (local-only defaults).

---

## What I would improve with more time

- **Decouple API query DTO from domain**: introduce a domain `QuerySpec` + `QueryMappers` in the API layer.
- **Domain exceptions vs HTTP exceptions**: let the domain throw `DomainConflict/DomainForbidden`, the API maps to 409/403/404 cleanly.
- **Metrics**: Micrometer + Prometheus/Grafana for storage latencies and Mongo timings.
- **Download filename**: serve the original filename on `Content-Disposition` instead of `download.bin`.
- **CI**: use GHCR + buildx with cache to avoid DockerHub rate-limit (and speed up builds).

---

## Short Q&A (reflection)

- **What did I enjoy the most?**  
  Turning a small set of requirements into a tidy **hexagonal** service where swapping infra is trivial felt great. The domain-first ports kept tests lean and fast.

- **Hardest decision?**  
  Choosing the thinnest viable surface for business rules (duplicates, ownership, visibility) without overengineering. Keeping controllers simple and pushing logic into the application layer.

- **If I had more time, what would I change first?**  
  The **query DTO** crossing layers and the **exception mapping** boundaries (see “What I would improve”). I’d also expand observability and polish CI/CD.

---
