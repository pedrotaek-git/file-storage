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
    private List<String> tagsNorm;

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
}
