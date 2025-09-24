package com.digitalarkcorp.filestorage.domain.ports;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;

import java.time.Instant;
import java.util.List;

public interface MetadataRepository {
    FileMetadata save(FileMetadata m);
    FileMetadata findById(String id);
    FileMetadata findByLinkId(String linkId);
    void rename(String id, String newFilename, Instant now);
    boolean deleteByIdAndOwner(String id, String ownerId);
    List<FileMetadata> listByOwner(String ownerId, ListQuery query);
    List<FileMetadata> listPublic(ListQuery query);
    boolean existsByOwnerAndFilename(String ownerId, String filename);
    boolean existsByOwnerAndContentHash(String ownerId, String contentHash);
    long countByContentHash(String contentHash);  // <— novo
}
