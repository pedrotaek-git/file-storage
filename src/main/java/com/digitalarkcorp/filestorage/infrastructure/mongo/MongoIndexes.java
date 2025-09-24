package com.digitalarkcorp.filestorage.infrastructure.mongo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

@Configuration
@RequiredArgsConstructor
public class MongoIndexes {

    private final MongoTemplate template;

    @PostConstruct
    public void ensure() {
        var col = template.getCollection("files");

        col.createIndex(
                Indexes.compoundIndex(Indexes.ascending("ownerId"), Indexes.ascending("filename")),
                new IndexOptions().name("uniq_owner_filename").unique(true)
        );
        col.createIndex(
                Indexes.compoundIndex(Indexes.ascending("ownerId"), Indexes.ascending("contentHash")),
                new IndexOptions().name("uniq_owner_contenthash").unique(true)
        );
        col.createIndex(Indexes.ascending("visibility"), new IndexOptions().name("ix_visibility"));
        col.createIndex(Indexes.ascending("tags"),       new IndexOptions().name("ix_tags"));
        col.createIndex(Indexes.ascending("filename"),   new IndexOptions().name("ix_filename"));
        col.createIndex(Indexes.ascending("contentHash"),new IndexOptions().name("ix_content_hash"));
        col.createIndex(Indexes.ascending("linkId"),     new IndexOptions().name("ix_link"));
        col.createIndex(Indexes.ascending("createdAt"),  new IndexOptions().name("ix_created_at"));
        col.createIndex(Indexes.ascending("updatedAt"),  new IndexOptions().name("ix_updated_at"));
    }
}
