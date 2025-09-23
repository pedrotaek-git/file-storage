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
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DefaultFileService implements FileService {

    private final MetadataRepository metadataRepository;
    private final StoragePort storagePort;

    public DefaultFileService(MetadataRepository metadataRepository, StoragePort storagePort) {
        this.metadataRepository = metadataRepository;
        this.storagePort = storagePort;
    }

    @Override
    public FileMetadata upload(String ownerId,
                               String filename,
                               Visibility visibility,
                               List<String> tags,
                               String contentType,
                               long size,
                               InputStream contentStream) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(contentStream);

        byte[] data;
        try {
            data = contentStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("read content", e);
        }
        String sha256 = sha256Hex(data);
        if (metadataRepository.existsByOwnerAndContentHash(ownerId, sha256)) {
            throw new ConflictException("same content for owner");
        }
        if (metadataRepository.existsByOwnerAndFilename(ownerId, filename)) {
            throw new ConflictException("duplicate filename for owner");
        }

        Instant now = Instant.now();
        FileMetadata toSave = new FileMetadata(
                null,
                ownerId,
                filename,
                visibility,
                FileQueries.normalizeTags(tags),
                size,
                contentType,
                sha256,
                UUID.randomUUID().toString(),
                FileStatus.READY,
                now,
                now
        );
        FileMetadata saved = metadataRepository.save(toSave);

        String objectKey = "f/" + saved.id();
        storagePort.put(objectKey, contentType, size, new ByteArrayInputStream(data));
        return saved;
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        return metadataRepository.listByOwner(ownerId, query);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        return metadataRepository.listPublic(query);
    }

    @Override
    public FileMetadata rename(String ownerId, String fileId, RenameRequest req) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(fileId);
        Objects.requireNonNull(req);
        String newName = req.newFilename();
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("new filename required");
        }

        FileMetadata current = metadataRepository.findById(fileId);
        if (current == null) throw new NotFoundException("file not found");
        if (!ownerId.equals(current.ownerId())) throw new ForbiddenException("not owner");
        if (metadataRepository.existsByOwnerAndFilename(ownerId, newName)) {
            throw new ConflictException("duplicate filename for owner");
        }

        Instant now = Instant.now();
        metadataRepository.rename(fileId, newName, now);
        FileMetadata updated = metadataRepository.findById(fileId);
        if (updated == null) throw new NotFoundException("file not found after rename");
        return updated;
    }

    @Override
    public void delete(String ownerId, String fileId) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(fileId);

        FileMetadata current = metadataRepository.findById(fileId);
        if (current == null) throw new NotFoundException("file not found");
        if (!ownerId.equals(current.ownerId())) throw new ForbiddenException("not owner");

        String objectKey = "f/" + current.id();
        storagePort.delete(objectKey);
        boolean ok = metadataRepository.deleteByIdAndOwner(fileId, ownerId);
        if (!ok) throw new NotFoundException("file not found on delete");
    }

    @Override
    public StoragePort.Resource downloadByLink(String linkId) {
        FileMetadata m = metadataRepository.findByLinkId(linkId);
        if (m == null) return null;
        String objectKey = "f/" + m.id();
        return storagePort.get(objectKey);
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(data);
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
