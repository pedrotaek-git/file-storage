package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.time.Instant;
import java.util.List;

public final class FileResponse {
    public final String id;
    public final String ownerId;
    public final String filename;
    public final Visibility visibility;
    public final List<String> tags;
    public final long size;
    public final String contentType;
    public final String linkId;
    public final FileStatus status;
    public final Instant createdAt;
    public final Instant updatedAt;

    public FileResponse(String id, String ownerId, String filename, Visibility visibility, List<String> tags,
                        long size, String contentType, String linkId, FileStatus status,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.filename = filename;
        this.visibility = visibility;
        this.tags = tags;
        this.size = size;
        this.contentType = contentType;
        this.linkId = linkId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FileResponse from(FileMetadata m) {
        return new FileResponse(
                m.id(),
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.linkId(),
                m.status(),
                m.createdAt(),
                m.updatedAt()
        );
    }
}
