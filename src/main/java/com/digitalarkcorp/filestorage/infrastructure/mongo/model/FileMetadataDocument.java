package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;

public class FileMetadataDocument {
    @Id
    private String id;
    private String ownerId;
    private String filename;
    private Visibility visibility;
    private List<String> tags;
    private long size;
    private String contentType;
    private String contentHash;
    private String linkId;
    private FileMetadata.FileStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static FileMetadataDocument fromDomain(FileMetadata m) {
        FileMetadataDocument d = new FileMetadataDocument();
        d.id = m.id();
        d.ownerId = m.ownerId();
        d.filename = m.filename();
        d.visibility = m.visibility();
        d.tags = m.tags();
        d.size = m.size();
        d.contentType = m.contentType();
        d.contentHash = m.contentHash();
        d.linkId = m.linkId();
        d.status = m.status();
        d.createdAt = m.createdAt();
        d.updatedAt = m.updatedAt();
        return d;
    }

    public FileMetadata toDomain() {
        return new FileMetadata(
                id, ownerId, filename, visibility, tags, size,
                contentType, contentHash, linkId, status, createdAt, updatedAt
        );
    }

    public void setFilename(String filename) { this.filename = filename; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
