package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class MongoFileMetadataRepository implements MetadataRepository {

    private final MongoTemplate mongo;

    public MongoFileMetadataRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        var q = new org.springframework.data.mongodb.core.query.Query();
        q.addCriteria(org.springframework.data.mongodb.core.query.Criteria
                .where("ownerId").is(ownerId)
                .and("contentHash").is(contentHash));
        return mongo.exists(q, FileMetadataDocument.class);
    }


    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query();
        q.addCriteria(Criteria.where("ownerId").is(ownerId));
        q.addCriteria(Criteria.where("filename").is(filename));
        return mongo.exists(q, FileMetadataDocument.class);
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument doc = FileMetadataDocument.fromDomain(m);
        try {
            FileMetadataDocument saved = mongo.save(doc);
            return saved.toDomain();
        } catch (DuplicateKeyException e) {
            throw e;
        }
    }

    @Override
    public FileMetadata findById(String id) {
        FileMetadataDocument d = mongo.findById(id, FileMetadataDocument.class);
        return d != null ? d.toDomain() : null;
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        Query q = new Query(Criteria.where("linkId").is(linkId));
        FileMetadataDocument d = mongo.findOne(q, FileMetadataDocument.class);
        return d != null ? d.toDomain() : null;
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        Query q = new Query(Criteria.where("ownerId").is(ownerId));
        applyListFilters(query, q);
        return mongo.find(q, FileMetadataDocument.class)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        Query q = new Query(Criteria.where("visibility").is(Visibility.PUBLIC.name()));
        applyListFilters(query, q);
        return mongo.find(q, FileMetadataDocument.class)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public void rename(String id, String newFilename, Instant updatedAt) {
        FileMetadataDocument d = mongo.findById(id, FileMetadataDocument.class);
        if (d == null) return;
        d = d.withFilename(newFilename).withUpdatedAt(updatedAt);
        mongo.save(d);
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("_id").is(id));
        q.addCriteria(Criteria.where("ownerId").is(ownerId));
        var res = mongo.remove(q, FileMetadataDocument.class);
        return res.getDeletedCount() > 0;
    }

    private static void applyListFilters(ListQuery query, Query q) {
        if (query == null) return;

        if (query.tag() != null && !query.tag().isBlank()) {
            q.addCriteria(Criteria.where("tags").regex(escapeRegex(query.tag()), "i"));
        }
        if (query.filenameContains() != null && !query.filenameContains().isBlank()) {
            q.addCriteria(Criteria.where("filename").regex(escapeRegex(query.filenameContains()), "i"));
        }

        int page = Math.max(0, query.page());
        int size = Math.max(1, query.size());
        q.skip((long) page * size).limit(size);

        String sortField = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case SIZE -> "size";
        };
        Sort.Direction dir = query.sortDir() == ListQuery.SortDir.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        q.with(Sort.by(dir, sortField));
    }

    private static String escapeRegex(String s) {
        return Objects.toString(s, "").replaceAll("([\\\\.\\[\\]\\{\\}\\(\\)\\*\\+\\?\\^\\$\\|])", "\\\\$1");
    }

    private String mapSortField(com.digitalarkcorp.filestorage.api.dto.ListQuery.SortBy by) {
        return switch (by) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case SIZE -> "size";
        };
    }

}
