package com.digitalarkcorp.filestorage.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String provider;
    private String localRoot;

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private boolean secure;
}
