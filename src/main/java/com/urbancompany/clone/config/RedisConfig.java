package com.urbancompany.clone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@Profile("prod")
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("services", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("service", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("serviceSearch", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("users", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("user", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("userByEmail", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("providers", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("provider", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("providersByService", config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("nearbyProviders", config.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("requests", config.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
