package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("files")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_owner_filename", def = "{'ownerId': 1, 'filename': 1}", unique = true),
        @CompoundIndex(name = "uniq_owner_contentHash", def = "{'ownerId': 1, 'contentHash': 1}", unique = true, sparse = true)
})
public class FileMetadataDocument {

    public static final class Fields {
        public static final String ID = "_id";
        public static final String OWNER_ID = "ownerId";
        public static final String FILENAME = "filename";
        public static final String VISIBILITY = "visibility";
        public static final String TAGS = "tags";
        public static final String TAGS_NORM = "tagsNorm";
        public static final String SIZE = "size";
        public static final String CONTENT_TYPE = "contentType";
        public static final String OBJECT_KEY = "objectKey";
        public static final String LINK_ID = "linkId";
        public static final String STATUS = "status";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        public static final String CONTENT_HASH = "contentHash";
    }

    @Id
    private String id;
    private String ownerId;
    private String filename;
    private Visibility visibility;
    private List<String> tags;
    private List<String> tagsNorm;
    private long size;
    private String contentType;
    private String objectKey;
    private String linkId;
    private FileStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String contentHash;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getTagsNorm() { return tagsNorm; }
    public void setTagsNorm(List<String> tagsNorm) { this.tagsNorm = tagsNorm; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }
    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
