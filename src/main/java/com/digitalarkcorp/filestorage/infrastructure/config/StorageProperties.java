package com.digitalarkcorp.filestorage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String endpoint,     // e.g. http://localhost:9000
        String accessKey,    // e.g. minioadmin
        String secretKey,    // e.g. minioadmin
        String bucket,       // e.g. files
        boolean secure       // true/false
) { }
