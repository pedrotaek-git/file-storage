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

    public FileMetadata upload(String ownerId, String filename, Visibility visibility,
                               List<String> tags, String contentType, long contentLength, InputStream data) {
        try {
            if (tags != null && tags.size() > 5) {
                throw new IllegalArgumentException("too many tags");
            }

            String normName = FileQueries.normalizeFilename(filename);

            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("fs-", ".bin");
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(tmp, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = data.read(buf)) != -1) {
                    md.update(buf, 0, r);
                    out.write(buf, 0, r);
                }
            }
            String hash = java.util.HexFormat.of().formatHex(md.digest());
            long size = java.nio.file.Files.size(tmp);

            if (repository.existsByOwnerAndFilename(ownerId, normName)) {
                java.nio.file.Files.deleteIfExists(tmp);
                throw new ConflictException("filename already exists for owner");
            }
            if (repository.existsByOwnerAndContentHash(ownerId, hash)) {
                java.nio.file.Files.deleteIfExists(tmp);
                throw new ConflictException("content already exists for owner");
            }

            if (contentType == null || contentType.isBlank()) {
                String probed = java.nio.file.Files.probeContentType(tmp);
                contentType = (probed != null) ? probed : "application/octet-stream";
            }

            String objectKey = hash;
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(tmp)) {
                storage.put(objectKey, in, size, contentType);
            } finally {
                java.nio.file.Files.deleteIfExists(tmp);
            }

            java.time.Instant now = java.time.Instant.now(clock);
            FileMetadata meta = new FileMetadata(
                    null, ownerId, normName, visibility, tags, size, contentType, hash,
                    java.util.UUID.randomUUID().toString(),
                    FileMetadata.FileStatus.READY, now, now);
            return repository.save(meta);
        } catch (java.io.IOException e) {
            throw new RuntimeException("upload io error", e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("sha256 error", e);
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
                    storage.delete(meta.contentHash());
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
