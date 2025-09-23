package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.application.util.FileQueries;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DefaultFileService implements FileService {

    private final MetadataRepository repository;
    private final StoragePort storage;
    private final Clock clock;

    public DefaultFileService(MetadataRepository repository, StoragePort storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public FileMetadata upload(String ownerId,
                               String filename,
                               Visibility visibility,
                               List<String> tags,
                               String contentType,
                               long contentLength,
                               InputStream data) {
        try {
            String normName = FileQueries.normalizeFilename(filename);
            byte[] bytes = data.readAllBytes();
            String hash = FileQueries.sha256(bytes);

            if (repository.existsByOwnerAndFilename(ownerId, normName)) {
                throw new IllegalStateException("duplicate filename");
            }
            if (repository.existsByOwnerAndContentHash(ownerId, hash)) {
                throw new IllegalStateException("duplicate content");
            }

            String objectKey = hash;
            storage.put(objectKey, new ByteArrayInputStream(bytes), contentLength, contentType);

            Instant now = Instant.now(clock);
            FileMetadata toSave = new FileMetadata(
                    null,
                    ownerId,
                    normName,
                    visibility,
                    tags,
                    contentLength,
                    contentType,
                    hash,
                    UUID.randomUUID().toString(),
                    FileMetadata.FileStatus.READY,
                    now,
                    now
            );
            return repository.save(toSave);
        } catch (IOException e) {
            throw new RuntimeException("upload stream read error", e);
        }
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        return repository.listByOwner(ownerId, query);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        return repository.listPublic(query);
    }

    @Override
    public FileMetadata rename(String userId, String id, RenameRequest req) {
        FileMetadata meta = repository.findById(id);
        if (meta == null) throw new IllegalArgumentException("file not found");
        if (!Objects.equals(meta.ownerId(), userId)) throw new SecurityException("not allowed");

        String newName = FileQueries.normalizeFilename(req.newFilename());
        if (repository.existsByOwnerAndFilename(userId, newName)) {
            throw new IllegalStateException("duplicate filename");
        }
        repository.rename(id, newName, Instant.now(clock));
        return repository.findById(id);
    }

    @Override
    public boolean delete(String userId, String id) {
        FileMetadata meta = repository.findById(id);
        if (meta == null) throw new IllegalArgumentException("file not found");
        if (!Objects.equals(meta.ownerId(), userId)) throw new SecurityException("not allowed");

        boolean deleted = repository.deleteByIdAndOwner(id, userId);
        if (deleted) {
            storage.delete(meta.contentHash()); // object key = hash
        }
        return deleted;
    }

    @Override
    public StoragePort.Resource getForDownload(String linkId) {
        FileMetadata meta = repository.findByLinkId(linkId);
        if (meta == null) throw new IllegalArgumentException("file not found");
        return storage.get(meta.contentHash());
    }
}
