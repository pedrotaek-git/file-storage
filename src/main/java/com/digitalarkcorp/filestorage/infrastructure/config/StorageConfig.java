package com.digitalarkcorp.filestorage.infrastructure.config;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.fs.LocalStorageAdapter;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class StorageConfig {

    @Bean
    public StoragePort storagePort(StorageProperties props, MinioClient minioClient) {
        String provider = props.getProvider() == null ? "local" : props.getProvider();
        if ("s3".equalsIgnoreCase(provider)) {
            return new com.digitalarkcorp.filestorage.infrastructure.s3.S3StorageAdapter(minioClient, props);
        }
        String localRootStr = props.getLocalRoot() == null ? "/tmp/filestorage" : props.getLocalRoot();
        return new LocalStorageAdapter(Path.of(localRootStr));
    }

    @Bean
    public MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
