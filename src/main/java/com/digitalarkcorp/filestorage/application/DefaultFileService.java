package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.ForbiddenException;
import com.digitalarkcorp.filestorage.api.errors.NotFoundException;
import com.digitalarkcorp.filestorage.application.util.FileQueries;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DefaultFileService implements FileService {

    private final MetadataRepository repository;
    private final StoragePort storagePort;

    public DefaultFileService(MetadataRepository repository, StoragePort storagePort) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.storagePort = Objects.requireNonNull(storagePort, "storagePort is required");
    }

    @Override
    public FileMetadata upload(String ownerId,
                               String filename,
                               Visibility visibility,
                               List<String> tags,
                               String contentType,
                               InputStream data,
                               long size) {

        if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("ownerId is required");
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("filename is required");
        if (visibility == null) throw new IllegalArgumentException("visibility is required");
        if (contentType == null || contentType.isBlank()) throw new IllegalArgumentException("contentType is required");
        if (data == null) throw new IllegalArgumentException("data is required");
        if (size < 0) throw new IllegalArgumentException("size must be >= 0");

        final String normalizedFilename = filename.trim();

        if (FileQueries.existsByOwnerAndFilename(repository, ownerId, normalizedFilename)) {
            throw new ConflictException("Filename already exists for this owner");
        }

        byte[] bytes = readAllBytes(data);
        String hash = sha256Hex(bytes);

        if (FileQueries.existsByOwnerAndHash(repository, ownerId, hash)) {
            throw new ConflictException("Duplicate content for this owner");
        }

        Instant now = Instant.now();
        String linkId = UUID.randomUUID().toString();

        FileMetadata toSave = new FileMetadata(
                null,                // id (gerado pelo Mongo)
                ownerId,
                normalizedFilename,
                visibility,
                tags,
                size,
                contentType,
                hash,
                linkId,
                FileStatus.READY,
                now,
                now
        );

        FileMetadata saved = repository.save(toSave);

        String objectKey = saved.id();
        String uploadId = storagePort.initiate(objectKey);
        storagePort.uploadPart(uploadId, 1, new ByteArrayInputStream(bytes), bytes.length);
        storagePort.complete(uploadId);

        return saved;
    }

    @Override
    public FileMetadata rename(String ownerId, String fileId, RenameRequest request) {
        if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("ownerId is required");
        if (fileId == null || fileId.isBlank()) throw new IllegalArgumentException("fileId is required");
        if (request == null || request.newFilename() == null || request.newFilename().isBlank()) {
            throw new IllegalArgumentException("newFilename is required");
        }

        FileMetadata current = repository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        if (!ownerId.equals(current.ownerId())) {
            throw new ForbiddenException("Not the owner");
        }

        String newName = request.newFilename().trim();
        if (newName.equals(current.filename())) {
            return current;
        }

        if (FileQueries.existsByOwnerAndFilename(repository, ownerId, newName)) {
            throw new ConflictException("Filename already exists for this owner");
        }

        FileMetadata updated = new FileMetadata(
                current.id(),
                current.ownerId(),
                newName,
                current.visibility(),
                current.tags(),
                current.size(),
                current.contentType(),
                current.contentHash(),
                current.linkId(),
                current.status(),
                current.createdAt(),
                Instant.now()
        );

        return repository.save(updated);
    }

    @Override
    public void delete(String ownerId, String fileId) {
        if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("ownerId is required");
        if (fileId == null || fileId.isBlank()) throw new IllegalArgumentException("fileId is required");

        FileMetadata current = repository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        if (!ownerId.equals(current.ownerId())) {
            throw new ForbiddenException("Not the owner");
        }

        storagePort.delete(current.id());
        repository.deleteById(fileId);
    }

    @Override
    public InputStream downloadByLink(String linkId) {
        if (linkId == null || linkId.isBlank()) throw new IllegalArgumentException("linkId is required");

        FileMetadata m = repository.findByLinkId(linkId)
                .orElseThrow(() -> new NotFoundException("Link not found"));

        return storagePort.get(m.id());
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        return repository.listPublic(
                query.tag(),
                query.sortBy(),
                query.sortDir(),
                query.page(),
                query.size()
        );
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        return repository.listByOwner(
                ownerId,
                query.tag(),
                query.sortBy(),
                query.sortDir(),
                query.page(),
                query.size()
        );
    }

    // Utils
    private static byte[] readAllBytes(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload stream", e);
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }
}
