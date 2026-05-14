package com.connectsphere.media.config;

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
 * <h1>RedisCacheConfig (Media Service)</h1>
 * <p>Configures Redis caching to optimize media retrieval and active story fetching.</p>
 * 
 * <h2>Caching Strategy Flow:</h2>
 * <pre>
 * graph TD
 *     A[Media/Story Request] --> B{Check Redis Cache}
 *     B -- Hit --> C[Return Media Data Fast]
 *     B -- Miss --> D[Fetch from DB/File System]
 *     D --> E[Cache with Custom TTL]
 *     E --> C
 * </pre>
 * 
 * <h2>Configuration Highlights:</h2>
 * <ul>
 *     <li><b>JSON Serialization:</b> Ensures objects are stored in human-readable JSON format.</li>
 *     <li><b>Custom TTLs:</b> Defines fast expiration (45s) for ephemeral content like active stories.</li>
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
                "mediaByPost", defaultConfig.entryTtl(Duration.ofMinutes(3)),
                "mediaById", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "mediaActiveStories", defaultConfig.entryTtl(Duration.ofSeconds(45)),
                "mediaByUserStories", defaultConfig.entryTtl(Duration.ofSeconds(45)),
                "mediaStoryById", defaultConfig.entryTtl(Duration.ofMinutes(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
