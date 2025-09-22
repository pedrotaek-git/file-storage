package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document("files")
@CompoundIndexes({
        @CompoundIndex(name = "ux_owner_filename", def = "{'ownerId':1,'filename':1}", unique = true),
        @CompoundIndex(name = "ux_owner_contentHash", def = "{'ownerId':1,'contentHash':1}", unique = true, sparse = true)
})
public class FileMetadataDocument {

    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String filename;

    @Indexed
    private Visibility visibility;

    private List<String> tags;

    @Indexed
    private List<String> tagsNorm; // lowercased copy of tags for case-insensitive filtering

    private long size;
    private String contentType;

    @Indexed(sparse = true)
    private String contentHash;

    @Indexed(sparse = true)
    private String linkId;

    private FileStatus status;

    @Indexed
    private Instant createdAt;

    @Indexed
    private Instant updatedAt;

    /** Centralized field names for queries (DRY and refactor-safe). */
    public static final class Fields {
        public static final String ID = "_id";
        public static final String OWNER_ID = "ownerId";
        public static final String FILENAME = "filename";
        public static final String VISIBILITY = "visibility";
        public static final String TAGS = "tags";
        public static final String TAGS_NORM = "tagsNorm";
        public static final String SIZE = "size";
        public static final String CONTENT_TYPE = "contentType";
        public static final String CONTENT_HASH = "contentHash";
        public static final String LINK_ID = "linkId";
        public static final String STATUS = "status";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        private Fields() {}
    }
}
