package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public FileService fileService(MetadataRepository repo, StoragePort storage) {
        return new DefaultFileService(repo, storage);
    }
}
