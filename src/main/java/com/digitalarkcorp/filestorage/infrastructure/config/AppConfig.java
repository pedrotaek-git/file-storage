package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PaginationProperties.class, StorageProperties.class})
public class AppConfig {

    @Bean
    FileService fileService(MetadataRepository repo, StoragePort storage) {
        return new DefaultFileService(repo, storage);
    }
}
