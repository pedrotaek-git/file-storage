package com.digitalarkcorp.filestorage.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable domain entity. Keep it small and explicit.
 * Changes return a new instance (withers).
 */
public record FileMetadata(
        String id,
        String ownerId,
        String filename,
        Visibility visibility,
        List<String> tags,
        long size,
        String contentType,
        String contentHash, // SHA-256
        String linkId,      // UUIDv7/ULID
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public FileMetadata {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (size < 0) throw new IllegalArgumentException("size must be >= 0");
    }

    public static FileMetadata pending(
            String ownerId,
            String filename,
            Visibility visibility,
            List<String> tags,
            Instant now
    ) {
        return new FileMetadata(
                null,                // id assigned by persistence
                ownerId,
                filename,
                visibility,
                tags,
                0L,                  // unknown yet
                null,                // unknown yet
                null,                // unknown yet
                null,                // link generated later
                FileStatus.PENDING,
                now,
                now
        );
    }

    public FileMetadata ready(long size, String contentType, String contentHash, String linkId, Instant now) {
        return new FileMetadata(
                this.id,
                this.ownerId,
                this.filename,
                this.visibility,
                this.tags,
                size,
                contentType,
                contentHash,
                linkId,
                FileStatus.READY,
                this.createdAt,
                now
        );
    }

    public FileMetadata withId(String id) {
        return new FileMetadata(
                id,
                this.ownerId,
                this.filename,
                this.visibility,
                this.tags,
                this.size,
                this.contentType,
                this.contentHash,
                this.linkId,
                this.status,
                this.createdAt,
                this.updatedAt
        );
    }

    public FileMetadata withFilename(String newFilename, Instant now) {
        return new FileMetadata(
                this.id,
                this.ownerId,
                newFilename,
                this.visibility,
                this.tags,
                this.size,
                this.contentType,
                this.contentHash,
                this.linkId,
                this.status,
                this.createdAt,
                now
        );
    }

    public FileMetadata withTags(List<String> newTags, Instant now) {
        return new FileMetadata(
                this.id,
                this.ownerId,
                this.filename,
                this.visibility,
                newTags,
                this.size,
                this.contentType,
                this.contentHash,
                this.linkId,
                this.status,
                this.createdAt,
                now
        );
    }
}
