package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.time.Instant;
import java.util.List;

public record FileResponse(
        String id,
        String ownerId,
        String filename,
        Visibility visibility,
        List<String> tags,
        long size,
        String contentType,
        String contentHash,
        String linkId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static FileResponse from(FileMetadata m) {
        return new FileResponse(
                m.id(),
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.contentHash(),
                m.linkId(),
                m.status().name(),
                m.createdAt(),
                m.updatedAt()
        );
    }
}
