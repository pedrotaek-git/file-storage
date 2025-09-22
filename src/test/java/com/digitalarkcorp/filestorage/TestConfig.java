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

@TestConfiguration
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
    public FileService fileService(MetadataRepository repo, StoragePort storage) {
        // Updated constructor: only (repo, storage)
        return new DefaultFileService(repo, storage);
    }

    @Bean
    public PaginationProperties paginationProperties() {
        // record PaginationProperties(Integer defaultSize, Integer maxSize)
        return new PaginationProperties(20, 100);
    }
}
