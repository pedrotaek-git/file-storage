package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.FileStatus;
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
        String linkId,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
