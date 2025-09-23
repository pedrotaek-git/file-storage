package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.mongo.MongoFileMetadataRepository;
import com.digitalarkcorp.filestorage.infrastructure.s3.S3StorageAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@EnableConfigurationProperties(PaginationProperties.class)
public class AppConfig {

    @Bean
    public MetadataRepository metadataRepository(MongoTemplate mongoTemplate) {
        return new MongoFileMetadataRepository(mongoTemplate);
    }

    @Bean
    public StoragePort storagePort() {
        return new S3StorageAdapter();
    }

    @Bean
    public FileService fileService(MetadataRepository metadataRepository, StoragePort storagePort) {
        return new DefaultFileService(metadataRepository, storagePort);
    }
}
