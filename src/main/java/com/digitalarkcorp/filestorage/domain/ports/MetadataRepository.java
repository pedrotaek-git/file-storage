package com.digitalarkcorp.filestorage.domain.ports;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;

import java.time.Instant;
import java.util.List;

public interface MetadataRepository {
    boolean existsByOwnerAndContentHash(String ownerId, String contentHash);
    boolean existsByOwnerAndFilename(String ownerId, String filename);

    FileMetadata save(FileMetadata m);

    FileMetadata findById(String id);

    FileMetadata findByLinkId(String linkId);

    List<FileMetadata> listByOwner(String ownerId, ListQuery query);

    List<FileMetadata> listPublic(ListQuery query);

    void rename(String id, String newFilename, Instant updatedAt);

    boolean deleteByIdAndOwner(String id, String ownerId);

}
