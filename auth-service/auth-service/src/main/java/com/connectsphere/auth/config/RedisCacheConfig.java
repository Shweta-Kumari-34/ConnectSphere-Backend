package com.connectsphere.auth.config;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration class for Redis caching in the Auth Service.
 * <p>
 * This class sets up the Redis connection factory, Redis template, and cache manager
 * to enable efficient caching of user profiles, search results, and verification eligibility.
 * It also defines a custom cache error handler to ensure that Redis failures do not crash
 * the application, allowing graceful fallback to database queries.
 * </p>
 *
 * <h3>Caching Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Layer] -->|@Cacheable Request| B{Cache Manager};
 *     B -->|Cache Hit| C[(Redis Cache)];
 *     B -->|Cache Miss| D[(MySQL DB)];
 *     D -->|Update Cache| C;
 *     B -.->|Redis Down| E[CacheErrorHandler];
 *     E -.->|Log Warning & Proceed| D;
 * </pre>
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "connectsphere.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    /**
     * Creates and configures the Redis connection factory.
     * 
     * @param host the Redis server host
     * @param port the Redis server port
     * @return the configured {@link RedisConnectionFactory}
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Configures the Redis template with custom JSON serializers for keys and values.
     * 
     * @param connectionFactory the Redis connection factory
     * @return the configured {@link RedisTemplate}
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configures the Redis cache manager with default and specific TTLs for various caches.
     * 
     * @param connectionFactory the Redis connection factory
     * @return the configured {@link RedisCacheManager}
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "authUserSearch", defaultConfig.entryTtl(Duration.ofSeconds(90)),
                "authAllUsers", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "authProfileByEmail", defaultConfig.entryTtl(Duration.ofMinutes(10)),
                "authProfileByUserId", defaultConfig.entryTtl(Duration.ofMinutes(10)),
                "authVerificationEligibility", defaultConfig.entryTtl(Duration.ofMinutes(3)),
                "authVerificationMine", defaultConfig.entryTtl(Duration.ofMinutes(2))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    /**
     * Provides a custom cache error handler to gracefully handle Redis connection issues.
     * 
     * @return the configured {@link CacheErrorHandler}
     */
    @Bean
    @ConditionalOnMissingBean(CacheErrorHandler.class)
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed for cache='{}', key='{}': {}", cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for cache='{}', key='{}': {}", cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed for cache='{}', key='{}': {}", cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed for cache='{}': {}", cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
