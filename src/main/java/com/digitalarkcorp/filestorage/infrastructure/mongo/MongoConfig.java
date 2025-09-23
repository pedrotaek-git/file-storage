package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Configuration
public class MongoConfig {

    private static final String COLLECTION = "files";
    private static final String UX_OWNER_FILENAME = "uniq_owner_filename";

    @Bean
    ApplicationRunner ensureMongoIndexes(MongoTemplate mongoTemplate, MongoDatabaseFactory dbFactory) {
        return args -> {
            if (!mongoTemplate.collectionExists(COLLECTION)) {
                mongoTemplate.createCollection(COLLECTION);
            }

            dedupeOwnerFilename(mongoTemplate);

            IndexOperations idxOps = mongoTemplate.indexOps(COLLECTION);
            Index ux = new Index()
                    .on("ownerId", Sort.Direction.ASC)
                    .on("filename", Sort.Direction.ASC)
                    .named(UX_OWNER_FILENAME)
                    .unique();

            try {
                idxOps.ensureIndex(ux);
            } catch (DuplicateKeyException e) {
                dedupeOwnerFilename(mongoTemplate);
                idxOps.ensureIndex(ux);
            }

            idxOps.ensureIndex(new Index().on("visibility", Sort.Direction.ASC).named("ix_visibility"));
            idxOps.ensureIndex(new Index().on("tags", Sort.Direction.ASC).named("ix_tags"));
            idxOps.ensureIndex(new Index().on("filename", Sort.Direction.ASC).named("ix_filename"));
            idxOps.ensureIndex(new Index().on("contentHash", Sort.Direction.ASC).named("ix_content_hash"));
            idxOps.ensureIndex(new Index().on("linkId", Sort.Direction.ASC).named("ix_link"));
        };
    }

    private void dedupeOwnerFilename(MongoTemplate mongoTemplate) {
        Query q = new Query().with(
                Sort.by(Sort.Order.asc("ownerId"), Sort.Order.asc("filename"),
                        Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"))
        );
        List<FileMetadataDocument> all = mongoTemplate.find(q, FileMetadataDocument.class, COLLECTION);

        Set<Pair> seen = new HashSet<>();
        for (FileMetadataDocument d : all) {
            Pair key = new Pair(d.getOwnerId(), d.getFilename());
            if (!seen.add(key)) {
                mongoTemplate.remove(new Query(where("_id").is(d.getId())), COLLECTION);
            }
        }
    }

    private record Pair(String ownerId, String filename) {
        Pair {
            ownerId = Objects.toString(ownerId, "");
            filename = Objects.toString(filename, "");
        }
    }
}
