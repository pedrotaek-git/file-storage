package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.application.util.FileQueries;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class MongoFileMetadataRepository implements MetadataRepository {

    private final MongoTemplate template;

    public MongoFileMetadataRepository(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument doc = FileMetadataDocument.fromDomain(m);
        template.save(doc, "files");
        return doc.toDomain();
    }

    @Override
    public FileMetadata findById(String id) {
        FileMetadataDocument d = template.findById(id, FileMetadataDocument.class, "files");
        return d == null ? null : d.toDomain();
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        Query q = new Query(where("linkId").is(linkId));
        FileMetadataDocument d = template.findOne(q, FileMetadataDocument.class, "files");
        return d == null ? null : d.toDomain();
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        Query q = new Query(where("_id").is(id));
        FileMetadataDocument doc = template.findOne(q, FileMetadataDocument.class, "files");
        if (doc == null) return;
        doc.setFilename(newFilename);
        doc.setUpdatedAt(now);
        template.save(doc, "files");
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        Query q = new Query(where("_id").is(id).and("ownerId").is(ownerId));
        return template.remove(q, "files").getDeletedCount() > 0;
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        Query q = new Query(where("ownerId").is(ownerId));
        applySearchAndSort(q, query);
        List<FileMetadataDocument> docs = template.find(q, FileMetadataDocument.class, "files");
        return docs.stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        Query q = new Query(where("visibility").is(Visibility.PUBLIC));
        applySearchAndSort(q, query);
        List<FileMetadataDocument> docs = template.find(q, FileMetadataDocument.class, "files");
        return docs.stream().map(FileMetadataDocument::toDomain).toList();
    }

    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query(where("ownerId").is(ownerId).and("filename").is(filename));
        return template.exists(q, "files");
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query(where("ownerId").is(ownerId).and("contentHash").is(contentHash));
        return template.exists(q, "files");
    }

    private void applySearchAndSort(Query q, ListQuery query) {
        if (query.tag() != null && !query.tag().isBlank()) {
            q.addCriteria(where("tags").regex("^" + FileQueries.escapeRegex(query.tag()) + "$", "i"));
        }
        if (query.q() != null && !query.q().isBlank()) {
            q.addCriteria(Criteria.where("filename").regex(FileQueries.escapeRegex(query.q()), "i"));
        }
        String sortField = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case SIZE -> "size";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };
        Sort.Direction dir = (query.sortDir() == ListQuery.SortDir.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;
        q.with(Sort.by(dir, sortField));
        q.with(PageRequest.of(Math.max(0, query.page()), Math.max(1, query.size())));
    }

    @Override
    public long countByContentHash(String contentHash) {
        Query q = new Query(Criteria.where("contentHash").is(contentHash));
        return template.count(q, "files");
    }

}
