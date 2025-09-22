package com.digitalarkcorp.filestorage.domain.ports;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;

import java.util.List;
import java.util.Optional;

public interface MetadataRepository {

    FileMetadata save(FileMetadata m);

    Optional<FileMetadata> findById(String id);

    Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename);

    Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash);

    void deleteById(String id);

    List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size);

    List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size);
}
