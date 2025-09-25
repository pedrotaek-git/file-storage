package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class MongoFileMetadataRepository implements MetadataRepository {

    private static final String COL = "files";

    private final MongoTemplate mongo;

    public MongoFileMetadataRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadataDocument d = new FileMetadataDocument(
                null,
                m.ownerId(),
                m.filename(),
                m.visibility().name(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.contentHash(),
                m.linkId(),
                m.status().name(),
                m.createdAt(),
                m.updatedAt()
        );

        d = mongo.insert(d, "files");

        return map(d);
    }

    private FileMetadata map(FileMetadataDocument d) {
        return new FileMetadata(
                d.id(),
                d.ownerId(),
                d.filename(),
                com.digitalarkcorp.filestorage.domain.Visibility.valueOf(d.visibility()),
                d.tags(),
                d.size(),
                d.contentType(),
                d.contentHash(),
                d.linkId(),
                com.digitalarkcorp.filestorage.domain.FileMetadata.FileStatus.valueOf(d.status()),
                d.createdAt(),
                d.updatedAt()
        );
    }

    @Override
    public FileMetadata findById(String id) {
        FileMetadataDocument d = mongo.findById(id, FileMetadataDocument.class, COL);
        return d == null ? null : map(d);
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        Query q = new Query(where("linkId").is(linkId));
        FileMetadataDocument d = mongo.findOne(q, FileMetadataDocument.class, COL);
        return d == null ? null : map(d);
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        Query q = new Query(Criteria.where("_id").is(id));
        Update u = new Update().set("filename", newFilename).set("updatedAt", now);
        mongo.updateFirst(q, u, "files");
    }


    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        Query q = new Query(where("_id").is(id).and("ownerId").is(ownerId));
        var res = mongo.remove(q, COL);
        return res.getDeletedCount() > 0;
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery query) {
        List<Criteria> ands = new ArrayList<>();
        ands.add(Criteria.where("ownerId").is(ownerId));

        if (hasText(query.tag())) {
            Pattern p = Pattern.compile("^" + Pattern.quote(query.tag()) + "$", Pattern.CASE_INSENSITIVE);
            ands.add(Criteria.where("tags").regex(p));
        }

        if (hasText(query.q())) {
            Pattern rx = Pattern.compile(Pattern.quote(query.q()), Pattern.CASE_INSENSITIVE);
            ands.add(Criteria.where("filename").regex(rx));
        }

        Query q = new Query(new Criteria().andOperator(ands.toArray(Criteria[]::new)));

        // sort
        String field = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case CONTENT_TYPE -> "contentType";
            default -> "createdAt";
        };
        q.with(Sort.by(query.sortDir() == ListQuery.SortDir.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, field));

        // page clamp
        int size = Math.min(Math.max(query.size(), 1), 100);
        int page = Math.max(query.page(), 0);
        q.skip((long) page * size).limit(size);

        var docs = mongo.find(q, FileMetadataDocument.class, COL);
        return docs.stream().map(this::map).toList();
    }

    private Query ownerQuery(String ownerId, ListQuery query) {
        Criteria c = Criteria.where("ownerId").is(ownerId);

        if (query.q() != null && !query.q().isBlank()) {
            String esc = Pattern.quote(query.q());
            c = new Criteria().andOperator(
                    Criteria.where("ownerId").is(ownerId),
                    Criteria.where("filename").regex(esc, "i")
            );
        }
        if (query.tag() != null && !query.tag().isBlank()) {
            String escTag = Pattern.quote(query.tag());
            c = new Criteria().andOperator(
                    c,
                    Criteria.where("tags").regex("^" + escTag + "$", "i")
            );
        }
        Query q = new Query(c);

        String field = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case CONTENT_TYPE -> "contentType";
            default -> "createdAt";
        };
        q.with(Sort.by(query.sortDir() == ListQuery.SortDir.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, field));

        int size = Math.min(Math.max(query.size(), 1), 100);
        int page = Math.max(query.page(), 0);
        q.skip((long) page * size).limit(size);
        return q;
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery query) {
        List<Criteria> ands = new ArrayList<>();
        ands.add(Criteria.where("visibility").is("PUBLIC"));

        if (hasText(query.tag())) {
            Pattern p = Pattern.compile("^" + Pattern.quote(query.tag()) + "$", Pattern.CASE_INSENSITIVE);
            ands.add(Criteria.where("tags").regex(p));
        }
        if (hasText(query.q())) {
            Pattern rx = Pattern.compile(Pattern.quote(query.q()), Pattern.CASE_INSENSITIVE);
            ands.add(Criteria.where("filename").regex(rx));
        }

        Query q = new Query(new Criteria().andOperator(ands.toArray(Criteria[]::new)));

        String field = switch (query.sortBy()) {
            case FILENAME -> "filename";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case CONTENT_TYPE -> "contentType";
            default -> "createdAt";
        };
        q.with(Sort.by(query.sortDir() == ListQuery.SortDir.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, field));

        int size = Math.min(Math.max(query.size(), 1), 100);
        int page = Math.max(query.page(), 0);
        q.skip((long) page * size).limit(size);

        var docs = mongo.find(q, FileMetadataDocument.class, COL);
        return docs.stream().map(this::map).toList();
    }


    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        Query q = new Query(where("ownerId").is(ownerId).and("filename").is(filename));
        return mongo.exists(q, FileMetadataDocument.class, COL);
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        Query q = new Query(where("ownerId").is(ownerId).and("contentHash").is(contentHash));
        return mongo.exists(q, FileMetadataDocument.class, COL);
    }

    @Override
    public long countByContentHash(String contentHash) {
        Query q = new Query(where("contentHash").is(contentHash));
        return mongo.count(q, FileMetadataDocument.class, COL);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static void applyFilters(Query q, ListQuery query) {
        if (hasText(query.q())) {
            Pattern rx = Pattern.compile(Pattern.quote(query.q()), Pattern.CASE_INSENSITIVE);
            q.addCriteria(where("filename").regex(rx));
        }
        if (hasText(query.tag())) {
            Pattern p = Pattern.compile("^" + Pattern.quote(query.tag()) + "$", Pattern.CASE_INSENSITIVE);
            q.addCriteria(where("tags").regex(p));
        }
    }

    private static void applyPagingAndSorting(Query q, ListQuery query) {
        // null-safe paging
        Integer pObj = query.page();
        Integer sObj = query.size();

        int page = (pObj == null || pObj < 0) ? 0 : pObj;
        int size = (sObj == null || sObj <= 0) ? 20 : Math.min(sObj, 100);

        q.skip((long) page * size).limit(size);

        // null-safe sort
        var by  = (query.sortBy()  != null) ? query.sortBy()  : ListQuery.SortBy.CREATED_AT;
        var dir = (query.sortDir() != null && query.sortDir() == ListQuery.SortDir.ASC)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        String field = switch (by) {
            case UPDATED_AT   -> "updatedAt";
            case FILENAME     -> "filename";
            case SIZE         -> "size";
            case CONTENT_TYPE -> "contentType";
            case CREATED_AT   -> "createdAt";
            case TAG          -> "tags";
            default           -> "createdAt";
        };

        q.with(Sort.by(dir, field));
    }
}
