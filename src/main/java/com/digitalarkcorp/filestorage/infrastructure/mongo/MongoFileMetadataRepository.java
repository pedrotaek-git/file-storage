package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.application.util.FileQueries;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class MongoFileMetadataRepository implements MetadataRepository {

    private static final String COLLECTION = "files";
    private final MongoTemplate mongo;

    public MongoFileMetadataRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument d = FileMetadataDocument.from(m);
        mongo.save(d, COLLECTION);
        return d.toDomain();
    }

    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query(where("ownerId").is(ownerId).and("filename").is(filename));
        return mongo.exists(q, COLLECTION);
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query(where("ownerId").is(ownerId).and("contentHash").is(contentHash));
        return mongo.exists(q, COLLECTION);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        Query q = new Query(where("ownerId").is(ownerId));
        applyFilters(q, query);
        applySort(q, query);
        q.skip((long) query.page() * query.size()).limit(query.size());
        return mongo.find(q, FileMetadataDocument.class, COLLECTION)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        Query q = new Query(where("visibility").is(Visibility.PUBLIC));
        applyFilters(q, query);
        applySort(q, query);
        q.skip((long) query.page() * query.size()).limit(query.size());
        return mongo.find(q, FileMetadataDocument.class, COLLECTION)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public FileMetadata findById(String id) {
        FileMetadataDocument d = mongo.findById(id, FileMetadataDocument.class, COLLECTION);
        return d == null ? null : d.toDomain();
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        Query q = new Query(where("linkId").is(linkId));
        FileMetadataDocument d = mongo.findOne(q, FileMetadataDocument.class, COLLECTION);
        return d == null ? null : d.toDomain();
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        FileMetadataDocument d = mongo.findById(id, FileMetadataDocument.class, COLLECTION);
        if (d == null) return;
        d.setFilename(newFilename);
        d.setUpdatedAt(now);
        mongo.save(d, COLLECTION);
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        Query q = new Query(where("_id").is(id).and("ownerId").is(ownerId));
        return mongo.remove(q, COLLECTION).getDeletedCount() > 0;
    }

    private void applyFilters(Query q, ListQuery query) {
        if (query.tag() != null && !query.tag().isBlank()) {
            q.addCriteria(where("tags").regex("^" + FileQueries.escapeRegex(query.tag()) + "$", "i"));
        }
        if (query.q() != null && !query.q().isBlank()) {
            q.addCriteria(Criteria.where("filename").regex(FileQueries.escapeRegex(query.q()), "i"));
        }
    }

    private void applySort(Query q, ListQuery query) {
        Sort.Direction dir = query.sortDir() == ListQuery.SortDir.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        String field;
        switch (query.sortBy()) {
            case FILENAME -> field = "filename";
            case CREATED_AT -> field = "createdAt";
            case TAG -> field = "tags";
            case CONTENT_TYPE -> field = "contentType";
            case SIZE -> field = "size";
            default -> field = "createdAt";
        }
        q.with(Sort.by(dir, field));
    }
}
