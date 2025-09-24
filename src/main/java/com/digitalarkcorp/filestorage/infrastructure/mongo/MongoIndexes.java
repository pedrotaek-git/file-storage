package com.digitalarkcorp.filestorage.infrastructure.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
@RequiredArgsConstructor
public class MongoIndexes {

    private final MongoTemplate template;

    @Bean
    ApplicationRunner ensureIndexes() {
        return args -> {
            IndexOperations ops = template.indexOps("files");

            ops.ensureIndex(new Index()
                    .on("ownerId", Sort.Direction.ASC)
                    .on("filename", Sort.Direction.ASC)
                    .unique()
                    .named("uniq_owner_filename"));

            ops.ensureIndex(new Index()
                    .on("ownerId", Sort.Direction.ASC)
                    .on("contentHash", Sort.Direction.ASC)
                    .unique()
                    .named("uniq_owner_contenthash"));

            ops.ensureIndex(new Index().on("visibility", Sort.Direction.ASC).named("ix_visibility"));
            ops.ensureIndex(new Index().on("tags", Sort.Direction.ASC).named("ix_tags"));
            ops.ensureIndex(new Index().on("filename", Sort.Direction.ASC).named("ix_filename"));
            ops.ensureIndex(new Index().on("contentHash", Sort.Direction.ASC).named("ix_content_hash"));
            ops.ensureIndex(new Index().on("linkId", Sort.Direction.ASC).named("ix_link"));
        };
    }
}
