package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.infrastructure.mongo.model.FileMetadataDocument;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    ApplicationRunner ensureMongoIndexes(MongoTemplate template) {
        return args -> {
            // Remove duplicates prior to unique index creation
            int removedOF = removeDuplicates(template, new String[]{"ownerId", "filename"});
            int removedOC = removeDuplicates(template, new String[]{"ownerId", "contentHash"});
            if (removedOF + removedOC > 0) {
                log.warn("Removed {} duplicates before index creation (owner+filename={}, owner+contentHash={}).",
                        removedOF + removedOC, removedOF, removedOC);
            }

            // Create indexes with a retry in case duplicates slip in
            createIndexesWithRetry(template);
        };
    }

    private void createIndexesWithRetry(MongoTemplate template) {
        IndexOperations io = template.indexOps(FileMetadataDocument.class);
        try {
            createIndexes(io);
        } catch (DuplicateKeyException e) {
            log.warn("DuplicateKeyException while creating indexes. Cleaning up and retrying. Cause: {}", e.getMessage());
            int r1 = removeDuplicates(template, new String[]{"ownerId", "filename"});
            int r2 = removeDuplicates(template, new String[]{"ownerId", "contentHash"});
            log.warn("Post-failure cleanup removed {} docs.", r1 + r2);
            createIndexes(io);
        }
    }

    private void createIndexes(IndexOperations io) {
        // Unique (ownerId, filename)
        io.ensureIndex(new Index()
                .on("ownerId", Sort.Direction.ASC)
                .on("filename", Sort.Direction.ASC)
                .unique()
                .named("ux_owner_filename"));

        // Unique (ownerId, contentHash)
        io.ensureIndex(new Index()
                .on("ownerId", Sort.Direction.ASC)
                .on("contentHash", Sort.Direction.ASC)
                .unique()
                .named("ux_owner_content"));

        // Unique linkId
        io.ensureIndex(new Index()
                .on("linkId", Sort.Direction.ASC)
                .unique()
                .named("ux_linkId"));

        // Helpful non-unique indexes
        io.ensureIndex(new Index()
                .on("visibility", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("ix_visibility_status"));

        io.ensureIndex(new Index()
                .on("tagsNorm", Sort.Direction.ASC)
                .named("ix_tagsNorm"));
    }

    /**
     * Removes duplicates by grouping on the given keys.
     * Keeps the most recent document (createdAt DESC) and deletes the rest.
     * Returns the number of removed documents.
     */
    @SuppressWarnings("unchecked")
    private int removeDuplicates(MongoTemplate template, String[] groupKeys) {
        final String collection = template.getCollectionName(FileMetadataDocument.class);

        List<AggregationOperation> pipeline = new ArrayList<>();
        // latest first
        pipeline.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")));
        // group by the provided keys; aggregate ids and count
        pipeline.add(Aggregation.group(groupKeys).push("_id").as("ids").count().as("count"));
        // keep only groups with more than one document
        pipeline.add(Aggregation.match(Criteria.where("count").gt(1)));
        // toDelete = all ids except the first (the first is the latest due to the initial sort)
        pipeline.add(Aggregation.project("ids", "count").andExpression("slice(ids, 1, count - 1)").as("toDelete"));

        Aggregation agg = Aggregation.newAggregation(pipeline);
        AggregationResults<Document> results = template.aggregate(agg, collection, Document.class);

        int removed = 0;
        for (Document d : results) {
            List<Object> toDelete = (List<Object>) d.get("toDelete");
            if (toDelete == null || toDelete.isEmpty()) continue;
            Query q = new Query(Criteria.where("_id").in(toDelete));
            var res = template.remove(q, FileMetadataDocument.class);
            // getDeletedCount() is a primitive long in current Spring Data; just cast to int
            removed += (int) res.getDeletedCount();
        }

        if (removed > 0) {
            log.warn("Removed {} duplicates from {} for keys {}.", removed, collection, String.join(",", groupKeys));
        }
        return removed;
    }
}
