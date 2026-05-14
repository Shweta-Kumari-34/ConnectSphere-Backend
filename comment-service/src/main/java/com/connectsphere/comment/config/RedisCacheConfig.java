package com.connectsphere.comment.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Configuration class for Redis caching in the Comment Service.
 * <p>
 * This class sets up the Redis connection, templates, and cache managers when caching
 * is enabled via properties. It improves performance by caching comment counts, 
 * paginated comment lists, and specific comment metadata.
 * </p>
 *
 * <h3>Caching Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Layer] -->|@Cacheable| B{Cache Manager};
 *     B -->|Hit| C[(Redis)];
 *     B -->|Miss| D[(MySQL DB)];
 *     D -->|Update| C;
 * </pre>
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "connectsphere.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheConfig {

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
     * Configures the Redis cache manager with default TTL and serialization settings.
     * 
     * @param connectionFactory the Redis connection factory
     * @return the configured {@link RedisCacheManager}
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }
}
