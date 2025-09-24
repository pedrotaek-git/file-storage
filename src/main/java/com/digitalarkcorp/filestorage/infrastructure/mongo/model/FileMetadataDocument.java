package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import java.time.Instant;
import java.util.List;

public record FileMetadataDocument(
        String id,
        String ownerId,
        String filename,
        String visibility,
        List<String> tags,
        long size,
        String contentType,
        String contentHash,
        String linkId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
