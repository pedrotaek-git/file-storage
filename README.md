# File Storage Service

A small **file storage** HTTP API I built with **Java 17 + Spring Boot 3**, **MongoDB**, and **MinIO (S3-compatible)**.  
It supports authenticated uploads (via a simple `X-User-Id` header), listing (owner and public), **tag filtering (case-insensitive)**, sorting + pagination, **download by link** with proper streaming headers, **rename**, and **delete** (which invalidates the download link).

> This repository is intentionally designed with a **hexagonal (ports & adapters)** flavor: the domain is isolated behind ports (`MetadataRepository`, `StoragePort`), infrastructure provides adapters (Mongo, S3/MinIO, local FS), and the web API is a thin layer.

---

## TL;DR (Quick Start)

**Prereqs:** Java 17+, Docker, Docker Compose, curl or Postman.

1) **Start dependencies** (Mongo + MinIO):

```bash
docker compose -f stack_local_environment/docker-compose.yml up -d
```

Wait until MinIO is healthy at <http://localhost:9000/minio/health/ready>.

2) **Build & run**:

```bash
./gradlew clean build
./gradlew bootRun
# or: java -jar build/libs/file-storage.jar
```

3) **Health**:

```bash
curl -sS http://localhost:8080/actuator/health | jq .
```

---

## Business Rules (what the API enforces)

- **Upload** requires header `X-User-Id` and a multipart body with JSON metadata (`filename`, `visibility`: `PUBLIC`/`PRIVATE`, optional `tags`) + file content.
- **Conflicts** (HTTP 409):
    - same owner **and** same `filename` → conflict.
    - same owner **and** **same content** (SHA-256) → conflict (even with a different filename).
    - different owners can upload the **same content** (no conflict).
- **List (owner)**: `GET /files` (requires `X-User-Id`). Optional `tag` (case-insensitive), `sortBy` (`FILENAME|SIZE|CREATED_AT`), `sortDir` (`ASC|DESC`), `page`, `size`.
- **List public**: `GET /files/public` with the same query model, but no auth header required.
- **Rename**: only the **owner** may rename (`PATCH /files/{id}/name`).
- **Delete**: only the **owner** may delete (`DELETE /files/{id}`); deletes also **invalidate the download link**.
- **Download**: `GET /d/{linkId}` returns content with headers:
    - `Content-Disposition: attachment; filename="download.bin"`
    - `Accept-Ranges: bytes`
    - `Content-Type`
    - `Content-Length`
    - `ETag: "{contentHash}"`
- **Validation**: missing `X-User-Id` → **400**; invalid filename (empty or ≥ 256 chars) → **400**.

> **Tags filter:** the API accepts any list of tags at upload; for listing we support **one `tag` filter** (case-insensitive).  
> If multi-tag queries become a hard requirement, I’d add a richer domain `QuerySpec` and map the HTTP DTO to it (see “Improvements”).

---

## Architecture (short)

- **Hexagonal**: domain (entities + services + ports) doesn’t depend on web/docs/infra.
- **Ports**: `MetadataRepository` (Mongo adapter), `StoragePort` (S3/MinIO or Local adapter).
- **API**: thin `FileController` + `DownloadController`. Global exception handler maps **business errors** to HTTP.
- **Observability**: Spring Boot Actuator; also exposes storage config via `/actuator/configprops` for quick verification.
- **Indexes** (Mongo): unique on `(ownerId, filename)` and `(ownerId, contentHash)`, plus helpful secondary indexes.

---

## How to validate with **Postman** (recommended)

1) **Import** the collection and environment from this repo:
    - `docs/postman/file-storage.postman_collection.json`
    - `docs/postman/file-storage.postman_environment.json`

2) **Set environment variables**:
    - `baseUrl` = `http://localhost:8080`
    - `owner1` = `u1`
    - `owner2` = `u2`

3) **Attach an actual file** for each “Upload …” request (the first time you run them).  
   I included two samples to avoid content-hash conflicts across runs:
    - `docs/samples/sample-A.txt` (hash: `3d0ec6fbdc6d551ee82d954011f0c5a66e2c0da932b3349f565cddc149ab1e46`)
    - `docs/samples/sample-B.txt` (hash: `0936305c0e3814234ef49ad30f83f734eb91f6515114a02771ad4cf12b471e45`)

   > **Why two files?** The domain rejects duplicate **content** for the same owner. If you re-run with the same file content, you’ll get `409 conflict`. Use the other sample or edit the file.

4) **Run the folder “01 – Happy Path”** in order; the collection scripts:
    - capture `id`, `linkId`, `contentHash` from Upload,
    - verify Listing (owner + public by tag),
    - Rename (authorized = 200, unauthorized = 403),
    - Download and assert streaming headers and `ETag == contentHash`,
    - Upload a PRIVATE file with tag `secret` and assert it **doesn’t** show in public listings,
    - negative checks (missing header = 400, duplicate name/content = 409),
    - delete and ensure link returns **404**,
    - pagination sanity.

> If you prefer **curl**, see `docs/curl/quick-check.sh` (it mirrors the Postman flow and was used to validate the app end-to-end).

---

## Running tests & coverage

```bash
./gradlew clean test
# HTML report:
open build/reports/tests/test/index.html
```

Unit tests focus on **domain/service behavior** (conflicts, rename auth, list filters, delete invalidates link) and a small API header test for the download controller.

---

## CI & Docker image

In CI I build & test with Gradle. Optionally, building a Docker image in CI validates the **Dockerfile** and produces a **reproducible artifact** you could push to a registry. It isn’t required to run locally (Compose already starts Mongo/MinIO), but it’s useful for deployments.

---

## Improvements I would make with more time

- Introduce a **domain-level QuerySpec** and map HTTP `ListQuery` to it (decouples API DTOs from domain and unlocks richer filters like multi-tag AND/OR).
- Raise **domain-specific exceptions** from the service and adapt them in the API layer.
- Byte-range (`206 Partial Content`) support for large downloads (Accept-Ranges is already announced).
- Add integration tests against real Mongo + MinIO (Testcontainers) on CI.
- Validation annotations/messages localized.
- Optional presigned URLs and link expiration.
- Replace simple `X-User-Id` header with proper auth in production.

---


## Requirements → Validation matrix

Below I map each requirement to how to validate it in this repo (via Postman or curl), and call out scope notes. Citations refer to the assignment PDF (see repo).

| Requirement | Where to validate | Notes |
|---|---|---|
| Upload with filename, visibility PUBLIC/PRIVATE, and tags (≤5) | Postman “01 – Happy Path → Upload PUBLIC (Demo)” and “Upload PRIVATE (secret)” | Tags are accepted and case-insensitive for filtering. I didn’t hard-enforce “≤5” (kept lean). |
| Rename filename | Postman “Rename (owner1)” (200) and “Rename unauthorized (owner2) => 403” | Unauthorized rename is blocked. |
| List files: all PUBLIC; all for USER | Postman “List PUBLIC (tag=dEmO)” and “List by Owner (tag=demo)” | Both endpoints exist and are covered. |
| Lists have filter by TAG (case-insensitive), sorting, pagination | Postman “List …” items; query params `tag`,`sortBy`,`sortDir`,`page`,`size` | I implemented sorting by **FILENAME**, **UPLOAD DATE** (createdAt), and **FILE SIZE**. Sorting by **TAG** and **CONTENT TYPE** are not implemented. Pagination supported. |
| Delete only by owner | Postman “Delete (owner1)” (200) and negative delete (403) | After delete, download link returns 404. |
| Detect file type after upload if not provided | Observed via response `contentType` | Present; full mime-sniffing left as small improvement. |
| Unique download link, downloadable for PUBLIC/PRIVATE | Postman “Download by link (check headers)” | Link is UUID; headers include `ETag`, `Content-Length`, etc. |
| Prevent duplicate upload by same owner by **filename** or **content** | Postman “Duplicate … => 409” items | Different owners can upload the same content (covered). |
| Non-functional: container memory ≤ 1GB; disk ≤ 200MB | See **Run with resource limits** | Memory via `--memory=1g`; disk layer limit varies by storage driver. |
| CI builds target Docker image | GitHub Actions | Image build step verifies Dockerfile. |

### Postman “Good practices” for this project
- Set environment variables first (baseUrl, owner1, owner2).
- For re-runs, switch the sample file (A ↔ B) or edit the file; duplicate content per owner is a **409**.
- The collection auto-captures `fileId`, `linkId`, `contentHash` and reuses them.
- Follow folder order; negative cases live in “02 – Negative / Edge Cases”.

## curl runner (mirrors Postman “Happy Path”)
Run:

```bash
bash docs/curl/quick-check.sh
```

It uploads, lists, renames, downloads (verifies ETag), uploads a private “secret”, tests authorization, deletes, validates link 404, and runs a small **concurrency** check (two parallel uploads of the same content).
