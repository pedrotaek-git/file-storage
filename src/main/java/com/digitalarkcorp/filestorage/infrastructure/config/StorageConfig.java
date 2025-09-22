package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.s3.S3StorageAdapter;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    @Bean
    public StoragePort storagePort(MinioClient client, StorageProperties props) {
        // S3StorageAdapter tem construtor (MinioClient, StorageProperties)
        return new S3StorageAdapter(client, props);
    }
}
