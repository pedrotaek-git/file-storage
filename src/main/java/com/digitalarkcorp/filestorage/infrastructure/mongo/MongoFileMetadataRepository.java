package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.application.util.FileQueries;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class MongoFileMetadataRepository implements MetadataRepository {

    private final MongoTemplate template;

    public MongoFileMetadataRepository(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument doc = FileMetadataDocument.fromDomain(m);
        template.save(doc);
        return doc.toDomain();
    }

    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query()
                .addCriteria(where("ownerId").is(ownerId))
                .addCriteria(where("filename").is(filename));
        return template.exists(q, FileMetadataDocument.class);
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query()
                .addCriteria(where("ownerId").is(ownerId))
                .addCriteria(where("contentHash").is(contentHash));
        return template.exists(q, FileMetadataDocument.class);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        Query q = new Query().addCriteria(where("ownerId").is(ownerId));
        applyCommonFiltersAndSorting(q, query);
        return template.find(q, FileMetadataDocument.class)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        Query q = new Query().addCriteria(where("visibility").is(com.digitalarkcorp.filestorage.domain.Visibility.PUBLIC));
        applyCommonFiltersAndSorting(q, query);
        return template.find(q, FileMetadataDocument.class)
                .stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public FileMetadata findById(String id) {
        FileMetadataDocument d = template.findById(id, FileMetadataDocument.class);
        return d == null ? null : d.toDomain();
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        Query q = new Query().addCriteria(where("linkId").is(linkId));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class);
        return d == null ? null : d.toDomain();
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        Query q = new Query().addCriteria(where("_id").is(id));
        Update u = new Update()
                .set("filename", newFilename)
                .set("updatedAt", now);
        template.updateFirst(q, u, FileMetadataDocument.class);
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        Query q = new Query()
                .addCriteria(where("_id").is(id))
                .addCriteria(where("ownerId").is(ownerId));
        return template.remove(q, FileMetadataDocument.class).getDeletedCount() > 0;
    }

    private void applyCommonFiltersAndSorting(Query q, ListQuery query) {
        if (query.tag() != null && !query.tag().isBlank()) {
            String exact = "^" + FileQueries.escapeRegex(query.tag()) + "$";
            q.addCriteria(where("tags").regex(exact, "i"));
        }
        if (query.q() != null && !query.q().isBlank()) {
            String rx = FileQueries.escapeRegex(query.q());
            q.addCriteria(Criteria.where("filename").regex(rx, "i"));
        }

        String sortField = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case SIZE -> "size";
        };
        Sort.Direction dir = (query.sortDir() == ListQuery.SortDir.DESC)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        q.with(Sort.by(dir, sortField));
        q.skip((long) query.page() * query.size()).limit(query.size());
    }
}
