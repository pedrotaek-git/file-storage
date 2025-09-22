package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
@EnableConfigurationProperties(PaginationProperties.class)
public class AppConfig {

    @Bean
    public FileService fileService(MetadataRepository repo,
                                   StoragePort storage,
                                   PaginationProperties paging) {
        return new DefaultFileService(repo, storage, paging);
    }
}
