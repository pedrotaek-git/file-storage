package com.digitalarkcorp.filestorage.infrastructure.mongo.model;

import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "files")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_owner_filename", def = "{'ownerId': 1, 'filename': 1}", unique = true),
        @CompoundIndex(name = "uniq_owner_content", def = "{'ownerId': 1, 'contentHash': 1}", unique = true, sparse = true)
})
public class FileMetadataDocument {
    @Id
    private String id;

    private String ownerId;
    private String filename;
    private Visibility visibility;

    private List<String> tags;       // raw tags, case may vary
    private List<String> tagsNorm;   // normalized to lowercase (for filtering)

    private long size;
    private String contentType;
    private String contentHash;      // optional, may be null
    private String objectKey;        // S3/MinIO object key
    private String linkId;           // public link id (UUID)
    private FileStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}
