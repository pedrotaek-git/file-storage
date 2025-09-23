package com.digitalarkcorp.filestorage.application.util;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;

import java.util.List;
import java.util.Objects;

public final class FileQueries {

    private static final int PAGE_SIZE = 500;

    private FileQueries() {}

    public static boolean existsByOwnerAndFilename(MetadataRepository repo, String ownerId, String filename) {
        Objects.requireNonNull(repo, "repo is required");
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filename, "filename is required");

        if (repo.findByOwnerAndFilename(ownerId, filename).isPresent()) {
            return true;
        }

        int page = 0;
        while (true) {
            List<FileMetadata> batch = repo.listByOwner(ownerId, null, SortBy.FILENAME, SortDir.ASC, page, PAGE_SIZE);
            if (batch == null || batch.isEmpty()) break;
            for (FileMetadata m : batch) {
                if (filename.equals(m.filename())) return true;
            }
            if (batch.size() < PAGE_SIZE) break; // última página
            page++;
        }
        return false;
    }

    public static boolean existsByOwnerAndHash(MetadataRepository repo, String ownerId, String contentHash) {
        Objects.requireNonNull(repo, "repo is required");
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(contentHash, "contentHash is required");

        if (repo.findByOwnerAndContentHash(ownerId, contentHash).isPresent()) {
            return true;
        }

        int page = 0;
        while (true) {
            List<FileMetadata> batch = repo.listByOwner(ownerId, null, SortBy.FILENAME, SortDir.ASC, page, PAGE_SIZE);
            if (batch == null || batch.isEmpty()) break;
            for (FileMetadata m : batch) {
                if (contentHash.equals(m.contentHash())) return true;
            }
            if (batch.size() < PAGE_SIZE) break;
            page++;
        }
        return false;
    }
}
