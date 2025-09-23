package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MongoFileMetadataRepository implements MetadataRepository {

    private final MongoTemplate template;

    public MongoFileMetadataRepository(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        try {
            FileMetadataDocument d = toDoc(m);
            // normalize tags into tagsNorm (lowercase) for filtering/sorting
            if (d.getTags() != null) {
                d.setTagsNorm(d.getTags().stream()
                        .map(s -> s == null ? null : s.toLowerCase(Locale.ROOT))
                        .toList());
            }
            FileMetadataDocument saved = template.save(d);
            return toDomain(saved);
        } catch (DuplicateKeyException dke) {
            // your ConflictException only accepts (String)
            throw new ConflictException("Duplicate key for unique constraint.");
        }
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        FileMetadataDocument d = template.findById(id, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query(Criteria.where("ownerId").is(ownerId).and("filename").is(filename));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query(Criteria.where("ownerId").is(ownerId).and("contentHash").is(contentHash));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public Optional<FileMetadata> findByLinkId(String linkId) {
        Query q = new Query(Criteria.where("linkId").is(linkId));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return Optional.ofNullable(d).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        template.remove(new Query(Criteria.where("_id").is(id)), FileMetadataDocument.class);
    }

    @Override
    public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        Criteria c = Criteria.where("visibility").is(Visibility.PUBLIC).and("status").is(FileStatus.READY);
        if (tag != null) c = c.and("tagsNorm").is(tag.toLowerCase(Locale.ROOT));

        Query q = new Query(c);
        applySortAndPage(q, sortBy, sortDir, page, size);

        return template.find(q, FileMetadataDocument.class)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        Criteria c = Criteria.where("ownerId").is(ownerId);
        if (tag != null) c = c.and("tagsNorm").is(tag.toLowerCase(Locale.ROOT));

        Query q = new Query(c);
        applySortAndPage(q, sortBy, sortDir, page, size);

        return template.find(q, FileMetadataDocument.class)
                .stream().map(this::toDomain).toList();
    }

    private void applySortAndPage(Query q, SortBy sortBy, SortDir sortDir, int page, int size) {
        Sort.Direction dir = (sortDir == SortDir.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String field = switch (sortBy) {
            case FILENAME     -> "filename";
            case UPLOAD_DATE  -> "createdAt";
            case TAG          -> "tagsNorm";
            case CONTENT_TYPE -> "contentType";
            case FILE_SIZE    -> "size";
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
