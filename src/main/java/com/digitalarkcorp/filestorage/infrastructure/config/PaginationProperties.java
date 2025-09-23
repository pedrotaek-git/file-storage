package com.digitalarkcorp.filestorage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(
        int defaultSize,
        int maxSize
) {}
