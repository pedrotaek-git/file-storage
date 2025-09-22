package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import com.digitalarkcorp.filestorage.testdouble.InMemoryMetadataRepository;
import com.digitalarkcorp.filestorage.testdouble.InMemoryStoragePort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public MetadataRepository metadataRepository() {
        return new InMemoryMetadataRepository();
    }

    @Bean
    public StoragePort storagePort() {
        return new InMemoryStoragePort();
    }

    @Bean
    public PaginationProperties paginationProperties() {
        // your record: (defaultSize, maxSize)
        return new PaginationProperties(20, 100);
    }

    @Bean
    public FileService fileService(MetadataRepository repo, StoragePort storage, PaginationProperties paging) {
        return new DefaultFileService(repo, storage, paging);
    }
}
