package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.errors.BadRequestException;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.ForbiddenException;
import com.digitalarkcorp.filestorage.api.errors.NotFoundException;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultFileService implements FileService {

    private final MetadataRepository repo;
    private final StoragePort storage;
    private final PaginationProperties paging;

    public DefaultFileService(MetadataRepository repo, StoragePort storage, PaginationProperties paging) {
        this.repo = repo;
        this.storage = storage;
        this.paging = paging;
    }

    @Override
    public FileMetadata upload(String ownerId, String filename, Visibility visibility, List<String> tags,
                               String contentType, InputStream data, long size) {
        if (ownerId == null || ownerId.isBlank()) throw new BadRequestException("Missing owner id");
        if (filename == null || filename.isBlank()) throw new BadRequestException("Missing filename");
        if (size < 0) throw new BadRequestException("Invalid size");

        List<String> normTags = normalizeTags(tags);

        repo.findByOwnerAndFilename(ownerId, filename).ifPresent(existing -> {
            throw new ConflictException("Filename already exists for this owner");
        });

        Instant now = Instant.now();
        String objectKey = UUID.randomUUID().toString();
        FileMetadata pending = new FileMetadata(
                null, ownerId, filename, visibility, normTags, size, contentType, objectKey,
                UUID.randomUUID().toString(), FileStatus.PENDING, now, now, null
        );
        FileMetadata savedPending = repo.save(pending);

        String uploadId = null;
        try {
            uploadId = storage.initiate(objectKey);
            storage.uploadPart(uploadId, 1, data, size);
            storage.complete(uploadId);

            String sha256 = computeSha256(objectKey, contentType, size);

            repo.findByOwnerAndContentHash(ownerId, sha256).ifPresent(dup -> {
                storage.delete(objectKey);
                throw new ConflictException("A file with the same content already exists for this owner");
            });

            FileMetadata ready = new FileMetadata(
                    savedPending.id(), ownerId, filename, visibility, normTags, size, contentType, objectKey,
                    savedPending.linkId(), FileStatus.READY, savedPending.createdAt(), Instant.now(), sha256
            );
            return repo.save(ready);

        } catch (RuntimeException ex) {
            if (uploadId != null) {
                try { storage.delete(objectKey); } catch (Exception ignored) {}
            }
            throw ex;
        }
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        Normalized nq = normalizeQuery(query);
        return repo.listPublic(nq.tag, nq.sortBy, nq.sortDir, nq.page, nq.size);
    }

    @Override
    public List<FileMetadata> listMine(String ownerId, ListQuery query) {
        if (ownerId == null || ownerId.isBlank()) throw new BadRequestException("Missing owner id");
        Normalized nq = normalizeQuery(query);
        return repo.listByOwner(ownerId, nq.tag, nq.sortBy, nq.sortDir, nq.page, nq.size);
    }

    @Override
    public FileMetadata rename(String ownerId, String id, String newFilename) {
        FileMetadata m = repo.findById(id).orElseThrow(() -> new NotFoundException("File not found"));
        if (!m.ownerId().equals(ownerId)) throw new ForbiddenException("Not the owner");
        if (newFilename == null || newFilename.isBlank()) throw new BadRequestException("Missing new filename");

        repo.findByOwnerAndFilename(ownerId, newFilename).ifPresent(existing -> {
            if (!existing.id().equals(id)) throw new ConflictException("Filename already exists for this owner");
        });

        FileMetadata renamed = new FileMetadata(
                m.id(), m.ownerId(), newFilename, m.visibility(), m.tags(), m.size(),
                m.contentType(), m.objectKey(), m.linkId(), m.status(), m.createdAt(), Instant.now(), m.contentHash()
        );
        return repo.save(renamed);
    }

    @Override
    public void delete(String ownerId, String id) {
        FileMetadata m = repo.findById(id).orElseThrow(() -> new NotFoundException("File not found"));
        if (!m.ownerId().equals(ownerId)) throw new ForbiddenException("Not the owner");
        storage.delete(m.objectKey());
        repo.deleteById(id);
    }

    @Override
    public InputStream downloadByLinkId(String linkId) {
        FileMetadata m = repo.findByLinkId(linkId).orElseThrow(() -> new NotFoundException("File not found"));
        return storage.get(m.objectKey());
    }

    @Override
    public FileMetadata findById(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("File not found"));
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) return null;
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    private Normalized normalizeQuery(ListQuery q) {
        int size = Math.min(q.size(), paging.maxSize());
        String tag = (q.tag() == null || q.tag().isBlank()) ? null : q.tag().trim().toLowerCase();
        return new Normalized(tag, q.sortBy(), q.sortDir(), q.page(), size);
    }

    private static String computeSha256(String objectKey, String contentType, long size) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((objectKey + "|" + contentType + "|" + size).getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new BadRequestException("SHA-256 not available");
        }
    }

    private record Normalized(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {}
}
