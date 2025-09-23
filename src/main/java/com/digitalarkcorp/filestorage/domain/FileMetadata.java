package com.digitalarkcorp.filestorage.domain;

import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.time.Instant;
import java.util.List;

public record FileMetadata(
        String id,
        String ownerId,
        String filename,
        Visibility visibility,
        List<String> tags,
        long size,
        String contentType,
        String contentHash,
        String linkId,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
