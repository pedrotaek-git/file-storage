package com.digitalarkcorp.filestorage.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
@EnableConfigurationProperties({
        PaginationProperties.class,
        StorageProperties.class
})
public class AppConfig { }
