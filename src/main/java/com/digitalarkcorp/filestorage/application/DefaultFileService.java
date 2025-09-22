package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
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

        // Reserve filename via PENDING; unique index enforces (ownerId, filename)
        FileMetadata pending = FileMetadata.pending(ownerId, filename, visibility, tags, now);
        FileMetadata savedPending;
        try {
            savedPending = metadataRepository.save(pending);
        } catch (DuplicateKeyException dup) {
            throw new ConflictException("Filename already exists for this owner");
        }

        final String objectKey = ownerId + "/" + savedPending.id();

        // Stream content and compute SHA-256 on the fly
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
            throw new RuntimeException("storage write failed", e);
        }

        final String contentHash = HexFormat.of().formatHex(sha256.digest());
        final String contentType = (providedContentType != null && !providedContentType.isBlank())
                ? providedContentType
                : MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

        // Content dedup per owner: pre-check
        if (metadataRepository.findByOwnerAndContentHash(ownerId, contentHash).isPresent()) {
            storagePort.delete(objectKey);
            metadataRepository.deleteById(savedPending.id());
            throw new ConflictException("A file with the same content already exists for this owner");
        }

        // Mark READY and guard race on final save
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
        throw new UnsupportedOperationException("rename: to be implemented");
    }

    @Override
    public void delete(String ownerId, String id) {
        throw new UnsupportedOperationException("delete: to be implemented");
    }

    @Override
    public FileMetadata findById(String id) {
        throw new UnsupportedOperationException("findById: to be implemented");
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
        throw new UnsupportedOperationException("downloadByLinkId: to be implemented");
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

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
