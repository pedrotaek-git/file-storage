package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "files")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_owner_filename", def = "{'ownerId': 1, 'filename': 1}", unique = true)
})
public class FileMetadataDocument {
    @Id
    private String id;
    private String ownerId;
    @Indexed
    private String filename;
    private String visibility;
    @Indexed
    private List<String> tags;
    private long size;
    private String contentType;
    @Indexed
    private String contentHash;
    @Indexed
    private String linkId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public FileMetadataDocument() {}

    public FileMetadataDocument(String id, String ownerId, String filename, String visibility, List<String> tags,
                                long size, String contentType, String contentHash, String linkId, String status,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.filename = filename;
        this.visibility = visibility;
        this.tags = tags;
        this.size = size;
        this.contentType = contentType;
        this.contentHash = contentHash;
        this.linkId = linkId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FileMetadataDocument fromDomain(FileMetadata m) {
        return new FileMetadataDocument(
                m.id(),
                m.ownerId(),
                m.filename(),
                m.visibility().name(),
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

    public FileMetadata toDomain() {
        return new FileMetadata(
                id,
                ownerId,
                filename,
                Visibility.valueOf(visibility),
                tags,
                size,
                contentType,
                contentHash,
                linkId,
                FileStatus.valueOf(status),
                createdAt,
                updatedAt
        );
    }

    public FileMetadataDocument withFilename(String newName) {
        return new FileMetadataDocument(id, ownerId, newName, visibility, tags, size, contentType, contentHash, linkId, status, createdAt, updatedAt);
    }

    public FileMetadataDocument withUpdatedAt(Instant t) {
        return new FileMetadataDocument(id, ownerId, filename, visibility, tags, size, contentType, contentHash, linkId, status, createdAt, t);
    }

    // Getters required by MongoConfig
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getFilename() { return filename; }
}
