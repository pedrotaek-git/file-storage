package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.ForbiddenException;
import com.digitalarkcorp.filestorage.api.errors.NotFoundException;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DefaultFileService implements FileService {

    private static final int BUFFER = 16 * 1024;

    private final MetadataRepository metadataRepository;
    private final StoragePort storagePort;
    private final PaginationProperties paginationProps;

    public DefaultFileService(MetadataRepository metadataRepository,
                              StoragePort storagePort,
                              PaginationProperties paginationProps) {
        this.metadataRepository = Objects.requireNonNull(metadataRepository);
        this.storagePort = Objects.requireNonNull(storagePort);
        this.paginationProps = Objects.requireNonNull(paginationProps);
    }

    @Override
    public FileMetadata upload(String ownerId,
                               String filename,
                               Visibility visibility,
                               List<String> tags,
                               String providedContentType,
                               InputStream data,
                               long contentLength) {

        final Instant now = Instant.now();

        FileMetadata pending = FileMetadata.pending(ownerId, filename, visibility, tags, now);
        FileMetadata savedPending;
        try {
            savedPending = metadataRepository.save(pending);
        } catch (DuplicateKeyException dup) {
            throw new ConflictException("Filename already exists for this owner");
        }

        final String objectKey = objectKey(ownerId, savedPending.id());

        final String uploadId = storagePort.initiate(objectKey);
        long totalBytes = 0L;
        final MessageDigest sha256 = newDigest();
        int part = 1;

        try {
            byte[] buf = new byte[BUFFER];
            while (true) {
                int n = data.read(buf);
                if (n == -1) break;
                sha256.update(buf, 0, n);
                totalBytes += n;
                storagePort.uploadPart(uploadId, part++, new java.io.ByteArrayInputStream(buf, 0, n), n);
            }
            storagePort.complete(uploadId);
        } catch (RuntimeException rethrown) {
            metadataRepository.deleteById(savedPending.id());
            throw rethrown;
        } catch (Exception e) {
            metadataRepository.deleteById(savedPending.id());
            throw new RuntimeException("Storage write failed", e);
        }

        final String contentHash = HexFormat.of().formatHex(sha256.digest());
        final String contentType = (providedContentType != null && !providedContentType.isBlank())
                ? providedContentType
                : MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

        if (metadataRepository.findByOwnerAndContentHash(ownerId, contentHash).isPresent()) {
            storagePort.delete(objectKey);
            metadataRepository.deleteById(savedPending.id());
            throw new ConflictException("A file with the same content already exists for this owner");
        }

        final String linkId = UUID.randomUUID().toString();
        FileMetadata ready = savedPending.ready(totalBytes, contentType, contentHash, linkId, Instant.now());
        try {
            return metadataRepository.save(ready);
        } catch (DuplicateKeyException dup) {
            storagePort.delete(objectKey);
            metadataRepository.deleteById(savedPending.id());
            throw new ConflictException("A file with the same content already exists for this owner");
        }
    }

    @Override
    public FileMetadata rename(String ownerId, String id, String newFilename) {
        FileMetadata existing = metadataRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        if (!existing.ownerId().equals(ownerId)) {
            throw new ForbiddenException("Not the owner");
        }
        if (existing.filename().equals(newFilename)) {
            return existing;
        }
        if (metadataRepository.findByOwnerAndFilename(ownerId, newFilename).isPresent()) {
            throw new ConflictException("Filename already exists for this owner");
        }
        FileMetadata updated = new FileMetadata(
                existing.id(),
                existing.ownerId(),
                newFilename,
                existing.visibility(),
                existing.tags(),
                existing.size(),
                existing.contentType(),
                existing.contentHash(),
                existing.linkId(),
                existing.status(),
                existing.createdAt(),
                Instant.now()
        );
        return metadataRepository.save(updated);
    }

    @Override
    public void delete(String ownerId, String id) {
        FileMetadata existing = metadataRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        if (!existing.ownerId().equals(ownerId)) {
            throw new ForbiddenException("Not the owner");
        }
        String objectKey = objectKey(ownerId, id);
        storagePort.delete(objectKey);
        metadataRepository.deleteById(id);
    }

    @Override
    public FileMetadata findById(String id) {
        return metadataRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        NormalizedQuery q = normalize(query);
        return metadataRepository.listPublic(q.tag, q.sortBy, q.sortDir, q.page, q.size);
    }

    @Override
    public List<FileMetadata> listMy(String ownerId, ListQuery query) {
        NormalizedQuery q = normalize(query);
        return metadataRepository.listByOwner(ownerId, q.tag, q.sortBy, q.sortDir, q.page, q.size);
    }

    @Override
    public InputStream downloadByLinkId(String linkId) {
        FileMetadata m = metadataRepository.findByLinkId(linkId)
                .orElseThrow(() -> new NotFoundException("Link not found"));
        String objectKey = objectKey(m.ownerId(), m.id());
        return storagePort.get(objectKey);
    }

    private String objectKey(String ownerId, String id) {
        return ownerId + "/" + id;
    }

    private NormalizedQuery normalize(ListQuery query) {
        if (query == null) {
            return new NormalizedQuery(null, SortBy.FILENAME, SortDir.ASC, 0, paginationProps.defaultSize());
        }
        final int page = (query.page() == null || query.page() < 0) ? 0 : query.page();
        final int requestedSize = (query.size() == null || query.size() < 1)
                ? paginationProps.defaultSize()
                : query.size();
        final int cappedSize = Math.min(requestedSize, paginationProps.maxSize());
        final SortBy sortBy = (query.sortBy() == null) ? SortBy.FILENAME : query.sortBy();
        final SortDir sortDir = (query.sortDir() == null) ? SortDir.ASC : query.sortDir();
        final String tag = (query.tag() == null || query.tag().isBlank()) ? null : query.tag();
        return new NormalizedQuery(tag, sortBy, sortDir, page, cappedSize);
    }

    private record NormalizedQuery(
            String tag,
            SortBy sortBy,
            SortDir sortDir,
            int page,
            int size
    ) {}

    private java.security.MessageDigest newDigest() {
        try {
            return java.security.MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
