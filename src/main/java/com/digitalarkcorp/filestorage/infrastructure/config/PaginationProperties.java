package com.digitalarkcorp.filestorage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(
        @Min(1) Integer defaultSize,
        @Min(1) Integer maxSize
) { }
