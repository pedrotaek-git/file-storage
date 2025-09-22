package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument.Fields.*;

public class MongoFileMetadataRepository implements MetadataRepository {

    private final MongoTemplate template;

    public MongoFileMetadataRepository(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument d = toDoc(m);
        if (d.getTags() != null) {
            d.setTagsNorm(d.getTags().stream()
                    .map(s -> s == null ? null : s.toLowerCase(Locale.ROOT))
                    .toList());
        }
        FileMetadataDocument saved = template.save(d);
        return toDomain(saved);
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        FileMetadataDocument d = template.findById(id, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query(Criteria.where(OWNER_ID).is(ownerId).and(FILENAME).is(filename));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query(Criteria.where(OWNER_ID).is(ownerId).and(CONTENT_HASH).is(contentHash));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByLinkId(String linkId) {
        Query q = new Query(Criteria.where(LINK_ID).is(linkId));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        template.remove(new Query(Criteria.where(ID).is(id)), FileMetadataDocument.class);
    }

    @Override
    public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        Criteria c = Criteria.where(VISIBILITY).is(Visibility.PUBLIC).and(STATUS).is(FileStatus.READY);
        if (tag != null) c = c.and(TAGS_NORM).is(tag.toLowerCase(Locale.ROOT));

        Query q = new Query(c);
        applySortAndPage(q, sortBy, sortDir, page, size);
        return template.find(q, FileMetadataDocument.class).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        Criteria c = Criteria.where(OWNER_ID).is(ownerId);
        if (tag != null) c = c.and(TAGS_NORM).is(tag.toLowerCase(Locale.ROOT));

        Query q = new Query(c);
        applySortAndPage(q, sortBy, sortDir, page, size);
        return template.find(q, FileMetadataDocument.class).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private void applySortAndPage(Query q, SortBy sortBy, SortDir sortDir, int page, int size) {
        Sort.Direction dir = (sortDir == SortDir.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String field = switch (sortBy) {
            case FILENAME     -> FILENAME;
            case UPLOAD_DATE  -> CREATED_AT;
            case TAG          -> TAGS_NORM;
            case CONTENT_TYPE -> CONTENT_TYPE;
            case FILE_SIZE    -> SIZE;
        };
        q.with(PageRequest.of(page, size, Sort.by(dir, field)));
    }

    private FileMetadataDocument toDoc(FileMetadata m) {
        FileMetadataDocument d = new FileMetadataDocument();
        d.setId(m.id());
        d.setOwnerId(m.ownerId());
        d.setFilename(m.filename());
        d.setVisibility(m.visibility());
        d.setTags(m.tags());
        d.setSize(m.size());
        d.setContentType(m.contentType());
        d.setContentHash(m.contentHash());
        d.setLinkId(m.linkId());
        d.setStatus(m.status());
        d.setCreatedAt(m.createdAt());
        d.setUpdatedAt(m.updatedAt());
        return d;
    }

    private FileMetadata toDomain(FileMetadataDocument d) {
        return new FileMetadata(
                d.getId(),
                d.getOwnerId(),
                d.getFilename(),
                d.getVisibility(),
                d.getTags(),
                d.getSize(),
                d.getContentType(),
                d.getContentHash(),
                d.getLinkId(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
