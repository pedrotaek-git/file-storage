package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoConfig {

    @Bean
    public MetadataRepository metadataRepository(MongoTemplate mongoTemplate) {
        ensureIndexes(mongoTemplate);
        return new MongoFileMetadataRepository(mongoTemplate);
    }

    private void ensureIndexes(MongoTemplate mongo) {
        IndexOperations ops = mongo.indexOps("files");
        ops.ensureIndex(new Index().on("ownerId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("filename", org.springframework.data.domain.Sort.Direction.ASC)
                .named("uniq_owner_filename").unique());
        ops.ensureIndex(new Index().on("ownerId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("contentHash", org.springframework.data.domain.Sort.Direction.ASC)
                .named("uniq_owner_contenthash").unique());
        ops.ensureIndex(new Index().on("visibility", org.springframework.data.domain.Sort.Direction.ASC).named("ix_visibility"));
        ops.ensureIndex(new Index().on("tags", org.springframework.data.domain.Sort.Direction.ASC).named("ix_tags"));
        ops.ensureIndex(new Index().on("filename", org.springframework.data.domain.Sort.Direction.ASC).named("ix_filename"));
        ops.ensureIndex(new Index().on("contentHash", org.springframework.data.domain.Sort.Direction.ASC).named("ix_content_hash"));
        ops.ensureIndex(new Index().on("linkId", org.springframework.data.domain.Sort.Direction.ASC).named("ix_link"));
    }
}
