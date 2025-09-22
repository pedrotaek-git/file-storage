package com.digitalarkcorp.filestorage.infrastructure.mongo;

import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

@Profile("!test")
@Configuration
public class MongoConfig {

    @Bean
    public MetadataRepository metadataRepository(MongoTemplate template) {
        return new MongoFileMetadataRepository(template);
    }
}
