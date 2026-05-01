package com.ims.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
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
 * Cache configuration with two modes:
 *
 * app.redis.enabled=true  → Redis-backed cache with per-cache TTLs
 * app.redis.enabled=false → In-memory ConcurrentMapCache (no TTL, suitable for Render free tier)
 *
 * All @Cacheable / @CacheEvict annotations in services work identically
 * in both modes — only the backing store changes.
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

    // ── Redis CacheManager (only when Redis is enabled) ───────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper())))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("products",   defaultConfig.entryTtl(Duration.ofSeconds(productTtl)));
        cacheConfigs.put("inventory",  defaultConfig.entryTtl(Duration.ofSeconds(120)));
        cacheConfigs.put("vendors",    defaultConfig.entryTtl(Duration.ofSeconds(vendorTtl)));
        cacheConfigs.put("warehouses", defaultConfig.entryTtl(Duration.ofSeconds(warehouseTtl)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig.entryTtl(Duration.ofSeconds(300)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    // ── In-memory CacheManager fallback (Redis disabled) ─────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager("products", "inventory", "vendors", "warehouses");
    }

    private ObjectMapper redisObjectMapper() {
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
