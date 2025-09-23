package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.s3.S3StorageAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public StoragePort storagePort() {
        return new S3StorageAdapter();
    }
}
