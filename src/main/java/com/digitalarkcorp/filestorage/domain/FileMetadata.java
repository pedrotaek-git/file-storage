package com.digitalarkcorp.filestorage.domain;

import java.time.Instant;
import java.util.List;

public final class FileMetadata {
    private final String id;
    private final String ownerId;
    private final String filename;
    private final Visibility visibility;
    private final List<String> tags;
    private final long size;
    private final String contentType;
    private final String objectKey;
    private final String linkId;
    private final FileStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String contentHash;

    public FileMetadata(
            String id,
            String ownerId,
            String filename,
            Visibility visibility,
            List<String> tags,
            long size,
            String contentType,
            String objectKey,
            String linkId,
            FileStatus status,
            Instant createdAt,
            Instant updatedAt,
            String contentHash
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.filename = filename;
        this.visibility = visibility;
        this.tags = tags;
        this.size = size;
        this.contentType = contentType;
        this.objectKey = objectKey;
        this.linkId = linkId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contentHash = contentHash;
    }

    public String id() { return id; }
    public String ownerId() { return ownerId; }
    public String filename() { return filename; }
    public Visibility visibility() { return visibility; }
    public List<String> tags() { return tags; }
    public long size() { return size; }
    public String contentType() { return contentType; }
    public String objectKey() { return objectKey; }
    public String linkId() { return linkId; }
    public FileStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public String contentHash() { return contentHash; }
}
