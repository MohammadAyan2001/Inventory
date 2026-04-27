package com.ims.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration.
 *
 * Strategy: Read-through caching via Spring @Cacheable.
 * - On cache miss, the service fetches from DB and populates the cache.
 * - On write/update, @CacheEvict invalidates the entry so the next read
 *   fetches fresh data (write-invalidate pattern).
 *
 * TTLs are per-cache to balance freshness vs. DB load:
 * - Products: 5 min (changes infrequently)
 * - Inventory: 2 min (changes on every order)
 * - Vendors/Warehouses: 10 min (rarely change)
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${app.cache.product-ttl:300}")
    private long productTtl;

    @Value("${app.cache.warehouse-ttl:600}")
    private long warehouseTtl;

    @Value("${app.cache.vendor-ttl:600}")
    private long vendorTtl;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofSeconds(productTtl)));
        cacheConfigs.put("inventory", defaultConfig.entryTtl(Duration.ofSeconds(120)));
        cacheConfigs.put("vendors", defaultConfig.entryTtl(Duration.ofSeconds(vendorTtl)));
        cacheConfigs.put("warehouses", defaultConfig.entryTtl(Duration.ofSeconds(warehouseTtl)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig.entryTtl(Duration.ofSeconds(300)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
