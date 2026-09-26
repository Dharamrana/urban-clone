package com.urbancompany.clone.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    // Redis cache configuration is handled by application.properties
    // Cache names are defined in RedisCacheManagerConfig
}
