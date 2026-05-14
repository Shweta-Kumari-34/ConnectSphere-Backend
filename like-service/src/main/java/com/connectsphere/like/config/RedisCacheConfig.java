package com.connectsphere.like.config;

import java.time.Duration;
import java.util.Map;

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
 * <h1>RedisCacheConfig</h1>
 * <p>Configures Redis as the primary caching mechanism for the Like Service to ensure 
 * high-performance read operations and reduce database load.</p>
 * 
 * <h2>Caching Architecture & Flow:</h2>
 * <pre>
 * graph TD
 *     A[Incoming Request] --> B{Cache Hit?}
 *     B -- Yes --> C[Return Cached Data]
 *     B -- No --> D[Query Database]
 *     D --> E[Store in Redis Cache]
 *     E --> F[Return Data]
 *     G[Update/Delete Operation] --> H[Evict Cache Entries]
 * </pre>
 * 
 * <h2>Configuration Details:</h2>
 * <ul>
 *     <li><b>Conditional Loading:</b> Activated only if {@code connectsphere.redis.enabled} is true.</li>
 *     <li><b>Serialization:</b> Uses {@link StringRedisSerializer} for keys and {@link GenericJackson2JsonRedisSerializer} for values.</li>
 *     <li><b>TTL Management:</b> Defines custom Time-To-Live (TTL) strategies for different cache regions (e.g., shorter TTL for reaction summaries).</li>
 * </ul>
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "connectsphere.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheConfig {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(config);
    }

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

    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "likeHasLiked", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "likeCount", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "likeCountByType", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "likeReactionSummary", defaultConfig.entryTtl(Duration.ofSeconds(90)),
                "likeByTarget", defaultConfig.entryTtl(Duration.ofSeconds(90)),
                "likeByUser", defaultConfig.entryTtl(Duration.ofMinutes(2))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
