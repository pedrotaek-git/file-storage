package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.NotFoundException;
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
            byte[] bytes = data.readAllBytes();
            String hash = FileQueries.sha256(bytes);
            String normName = FileQueries.normalizeFilename(filename);

            if (repository.existsByOwnerAndFilename(ownerId, normName)) {
                throw new ConflictException("filename already exists for owner");
            }
            if (repository.existsByOwnerAndContentHash(ownerId, hash)) {
                throw new ConflictException("content already exists for owner");
            }

            String objectKey = hash;
            storage.put(objectKey, new ByteArrayInputStream(bytes), contentLength, contentType);

            Instant now = Instant.now(clock);
            FileMetadata meta = new FileMetadata(
                    null,
                    ownerId,
                    normName,
                    visibility,
                    tags,
                    bytes.length,
                    contentType,
                    hash,
                    UUID.randomUUID().toString(),
                    FileMetadata.FileStatus.READY,
                    now,
                    now
            );
            return repository.save(meta);
        } catch (IOException e) {
            throw new RuntimeException("upload io error", e);
        }
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        return repository.listPublic(query);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        return repository.listByOwner(ownerId, query);
    }

    @Override
    public FileMetadata rename(String userId, String id, RenameRequest req) {
        FileMetadata meta = repository.findById(id);
        if (meta == null) throw new NotFoundException("file not found");
        if (!Objects.equals(meta.ownerId(), userId)) {
            throw new SecurityException("not owner");
        }
        String newName = FileQueries.normalizeFilename(req.newFilename());
        if (repository.existsByOwnerAndFilename(userId, newName)) {
            throw new ConflictException("filename already exists for owner");
        }
        repository.rename(id, newName, Instant.now(clock));
        return repository.findById(id);
    }

    @Override
    public boolean delete(String userId, String id) {
        FileMetadata meta = repository.findById(id);
        if (meta == null) throw new NotFoundException("file not found");
        if (!Objects.equals(meta.ownerId(), userId)) {
            throw new SecurityException("not owner");
        }
        boolean deleted = repository.deleteByIdAndOwner(id, userId);
        if (deleted) {
            long remaining = repository.countByContentHash(meta.contentHash());
            if (remaining == 0) {
                try {
                    storage.delete(meta.contentHash()); // tolera ausência no backend
                } catch (RuntimeException ignored) {
                }
            }
        }
        return deleted;
    }

    @Override
    public StoragePort.Resource getForDownload(String linkId) {
        FileMetadata meta = repository.findByLinkId(linkId);
        if (meta == null) throw new NotFoundException("file not found");
        return storage.get(meta.contentHash());
    }
}
