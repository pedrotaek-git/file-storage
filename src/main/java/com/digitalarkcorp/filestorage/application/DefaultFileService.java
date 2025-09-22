package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultFileService implements FileService {

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
        // to be implemented next: PENDING→READY, SHA-256, dedup, S3 multipart
        throw new UnsupportedOperationException("upload: to be implemented");
    }

    @Override
    public FileMetadata rename(String ownerId, String id, String newFilename) {
        // to be implemented next: ownership check + unique (ownerId, filename)
        throw new UnsupportedOperationException("rename: to be implemented");
    }

    @Override
    public void delete(String ownerId, String id) {
        // to be implemented next: ownership check + delete metadata/object
        throw new UnsupportedOperationException("delete: to be implemented");
    }

    @Override
    public FileMetadata findById(String id) {
        // to be implemented next: find or 404
        throw new UnsupportedOperationException("findById: to be implemented");
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        NormalizedQuery q = normalize(query);
        // return metadataRepository.listPublic(q.tag, q.sortBy, q.sortDir, q.page, q.size);
        throw new UnsupportedOperationException("listPublic: to be implemented");
    }

    @Override
    public List<FileMetadata> listMy(String ownerId, ListQuery query) {
        NormalizedQuery q = normalize(query);
        // return metadataRepository.listByOwner(ownerId, q.tag, q.sortBy, q.sortDir, q.page, q.size);
        throw new UnsupportedOperationException("listMy: to be implemented");
    }

    @Override
    public InputStream downloadByLinkId(String linkId) {
        // to be implemented next: resolve linkId → objectKey, stream from storage
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
}
