package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
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
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultFileService implements FileService {

    private final MetadataRepository repo;
    private final StoragePort storage;

    public DefaultFileService(MetadataRepository repo, StoragePort storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Override
    public FileMetadata upload(String ownerId,
                               String filename,
                               Visibility visibility,
                               List<String> tags,
                               String contentType,
                               long contentLength,
                               InputStream data) {
        byte[] bytes;
        try {
            bytes = data.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String contentHash = sha256(bytes);

        if (repo.existsByOwnerAndFilename(ownerId, filename)) {
            throw new RuntimeException("same name for owner");
        }
        if (repo.existsByOwnerAndContentHash(ownerId, contentHash)) {
            throw new RuntimeException("same content for owner");
        }

        String id = randomId();
        String linkId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        List<String> normTags = normalize(tags);

        FileMetadata meta = new FileMetadata(
                id,
                ownerId,
                filename,
                visibility,
                normTags,
                contentLength,
                contentType,
                contentHash,
                linkId,
                FileMetadata.FileStatus.READY,
                now,
                now
        );

        storage.put(id, new ByteArrayInputStream(bytes), contentLength, contentType);
        repo.save(meta);
        return meta;
    }

    @Override
    public FileMetadata rename(String ownerId, String id, RenameRequest req) {
        FileMetadata current = repo.findById(id);
        if (current == null) {
            throw new RuntimeException("not found");
        }
        if (!current.ownerId().equals(ownerId)) {
            throw new RuntimeException("not owner");
        }
        if (repo.existsByOwnerAndFilename(ownerId, req.newFilename())) {
            throw new RuntimeException("same name for owner");
        }
        Instant now = Instant.now();
        repo.rename(id, req.newFilename(), now);
        return repo.findById(id);
    }

    @Override
    public void delete(String ownerId, String id) {
        FileMetadata m = repo.findById(id);
        if (m == null) {
            return;
        }
        if (!m.ownerId().equals(ownerId)) {
            return;
        }
        boolean deleted = repo.deleteByIdAndOwner(id, ownerId);
        if (deleted) {
            storage.delete(id);
        }
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery q) {
        return repo.listByOwner(ownerId, q);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery q) {
        return repo.listPublic(q);
    }

    @Override
    public StoragePort.Resource downloadByLink(String linkId) {
        FileMetadata m = repo.findByLinkId(linkId);
        if (m == null) {
            throw new RuntimeException("not found");
        }
        return storage.get(m.id());
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> normalize(List<String> tags) {
        if (tags == null) return null;
        return tags.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    private static String randomId() {
        return Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits(), 36)
                + Long.toUnsignedString(UUID.randomUUID().getLeastSignificantBits(), 36);
    }
}
