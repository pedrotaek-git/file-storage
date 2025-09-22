package com.digitalarkcorp.filestorage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(
        Integer defaultSize,
        Integer maxSize
) {
    public PaginationProperties {
        if (defaultSize == null || defaultSize < 1) defaultSize = 20;
        if (maxSize == null || maxSize < 1) maxSize = 100;
    }
}
