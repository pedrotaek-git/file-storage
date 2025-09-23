package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class MongoConfig {

    public static final String COLLECTION = "files";

    @Bean
    CommandLineRunner ensureMongoIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations ix = mongoTemplate.indexOps(FileMetadataDocument.class);

            // unique (ownerId, filename)
            ix.ensureIndex(new Index().on("ownerId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("filename", org.springframework.data.domain.Sort.Direction.ASC)
                    .named("uniq_owner_filename").unique());

            // unique (ownerId, contentHash) -> dedupe por conteúdo para o mesmo owner
            ix.ensureIndex(new Index().on("ownerId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("contentHash", org.springframework.data.domain.Sort.Direction.ASC)
                    .named("uniq_owner_contenthash").unique());

            // queries
            ix.ensureIndex(new Index().on("visibility", org.springframework.data.domain.Sort.Direction.ASC).named("ix_visibility"));
            ix.ensureIndex(new Index().on("tags", org.springframework.data.domain.Sort.Direction.ASC).named("ix_tags"));
            ix.ensureIndex(new Index().on("filename", org.springframework.data.domain.Sort.Direction.ASC).named("ix_filename"));
            ix.ensureIndex(new Index().on("contentHash", org.springframework.data.domain.Sort.Direction.ASC).named("ix_content_hash"));
            ix.ensureIndex(new Index().on("linkId", org.springframework.data.domain.Sort.Direction.ASC).named("ix_link"));

            // optional: cleanup duplicated docs conflicting with the newly-created unique indexes
            removeDuplicatesForIndex(mongoTemplate, "uniq_owner_filename", "ownerId", "filename");
            removeDuplicatesForIndex(mongoTemplate, "uniq_owner_contenthash", "ownerId", "contentHash");
        };
    }

    private void removeDuplicatesForIndex(MongoTemplate mongoTemplate, String indexName, String fieldA, String fieldB) {
        // Find duplicates based on (fieldA, fieldB) and remove older ones, keep the first
        // Safe no-op if none exist
        List<Document> all = mongoTemplate.getCollection(COLLECTION)
                .find().projection(new Document("_id", 1).append(fieldA, 1).append(fieldB, 1))
                .into(new java.util.ArrayList<>());

        Set<String> seen = new HashSet<>();
        for (Document d : all) {
            String key = d.getString(fieldA) + "||" + d.getString(fieldB);
            if (seen.contains(key)) {
                mongoTemplate.getCollection(COLLECTION).deleteOne(new Document("_id", d.getObjectId("_id")));
            } else {
                seen.add(key);
            }
        }
    }
}
