package com.connectsphere.search.service.impl;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.service.SearchService;
import com.connectsphere.search.util.SearchConstants;
import com.connectsphere.search.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h1>SearchServiceImpl</h1>
 * <p>Implementation of {@link SearchService} utilizing advanced regex parsing and 
 * extensive caching for high-performance hashtag discovery.</p>
 * 
 * <h2>Caching Strategy & Extraction:</h2>
 * <pre>
 * sequenceDiagram
 *     Actor->>SearchService: indexPost(content)
 *     SearchService->>Regex: parse(#hashtags)
 *     SearchService->>DB: Save Hashtag Entities
 *     Note over SearchService: Evict existing caches to ensure freshness
 *     Actor->>SearchService: getTrendingHashtags()
 *     SearchService->>SpringCache: Check 'trendingHashtags'
 *     SpringCache-->>Actor: Return Cached Result
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Regex Parsing:</b> Automatically extracts valid hashtag structures from raw text.</li>
 *     <li><b>High-Performance Reads:</b> Implements {@code @Cacheable} across all read operations.</li>
 *     <li><b>Atomic Eviction:</b> Uses complex {@code @Caching(evict=...)} rules to maintain cache consistency.</li>
 *     <li><b>Aggregation:</b> Queries the database for trending logic using custom grouping queries.</li>
 * </ul>
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final HashtagRepository hashtagRepository;

    public SearchServiceImpl(HashtagRepository hashtagRepository) {
        this.hashtagRepository = hashtagRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "searchByTag", allEntries = true),
            @CacheEvict(value = "searchPosts", allEntries = true),
            @CacheEvict(value = "getHashtagsForPost", allEntries = true),
            @CacheEvict(value = "trendingHashtags", allEntries = true),
            @CacheEvict(value = "postsByHashtag", allEntries = true),
            @CacheEvict(value = "searchHashtags", allEntries = true),
            @CacheEvict(value = "hashtagCount", allEntries = true),
            @CacheEvict(value = "allHashtags", allEntries = true)
    })
    public void indexPost(Long postId, String content) {
        // Parse #hashtag tokens from post content and index them
        if (content == null) return;
        Matcher matcher = SearchConstants.HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            indexHashtag(tag, postId);
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "getHashtagsForPost", key = "#a0"),
            @CacheEvict(value = "searchByTag", allEntries = true),
            @CacheEvict(value = "searchPosts", allEntries = true),
            @CacheEvict(value = "trendingHashtags", allEntries = true),
            @CacheEvict(value = "postsByHashtag", allEntries = true),
            @CacheEvict(value = "searchHashtags", allEntries = true),
            @CacheEvict(value = "hashtagCount", allEntries = true),
            @CacheEvict(value = "allHashtags", allEntries = true)
    })
    public void removePostIndex(Long postId) {
        List<Hashtag> hashtags = hashtagRepository.findByPostId(postId);
        hashtagRepository.deleteAll(hashtags);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "searchByTag", allEntries = true),
            @CacheEvict(value = "searchPosts", allEntries = true),
            @CacheEvict(value = "trendingHashtags", allEntries = true),
            @CacheEvict(value = "postsByHashtag", allEntries = true),
            @CacheEvict(value = "searchHashtags", allEntries = true),
            @CacheEvict(value = "hashtagCount", allEntries = true),
            @CacheEvict(value = "allHashtags", allEntries = true)
    })
    public Hashtag indexHashtag(String tag, Long postId) {
        Hashtag hashtag = new Hashtag();
        hashtag.setTag(tag.toLowerCase().trim());
        hashtag.setPostId(postId);
        hashtag.setCreatedAt(LocalDateTime.now());
        return hashtagRepository.save(hashtag);
    }

    @Override
    @Cacheable(value = "searchByTag", key = "#a0", unless = "#result == null")
    public List<Hashtag> searchByTag(String tag) {
        return hashtagRepository.findByTagContainingIgnoreCase(tag);
    }

    @Override
    @Cacheable(value = "searchPosts", key = "#a0", unless = "#result == null")
    public List<Long> searchPosts(String keyword) {
        // Return post IDs that have hashtags matching the keyword
        return hashtagRepository.findByTagContainingIgnoreCase(keyword)
                .stream().map(Hashtag::getPostId).distinct().collect(Collectors.toList());
    }

    @Override
    public List<String> searchUsers(String keyword) {
        // Placeholder — user search would require cross-service call to auth-service
        // For now returns empty; in production, use RestTemplate or Feign to call auth-service
        return List.of();
    }

    @Override
    @Cacheable(value = "getHashtagsForPost", key = "#a0", unless = "#result == null")
    public List<Hashtag> getHashtagsForPost(Long postId) {
        return hashtagRepository.findByPostId(postId);
    }

    @Override
    @Cacheable(value = "trendingHashtags", key = "#a0", unless = "#result == null")
    public List<Object[]> getTrendingHashtags(int limit) {
        List<Object[]> all = hashtagRepository.findTrendingHashtags();
        return all.stream().limit(limit).toList();
    }

    @Override
    @Cacheable(value = "postsByHashtag", key = "#a0", unless = "#result == null")
    public List<Long> getPostsByHashtag(String tag) {
        return hashtagRepository.findByTagIgnoreCase(tag)
                .stream().map(Hashtag::getPostId).distinct().collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "searchHashtags", key = "#a0", unless = "#result == null")
    public List<Hashtag> searchHashtags(String keyword) {
        return hashtagRepository.findByTagContainingIgnoreCase(keyword);
    }

    @Override
    @Cacheable(value = "hashtagCount", key = "#a0")
    public long getHashtagCount(String tag) {
        return hashtagRepository.countByTagIgnoreCase(tag);
    }

    @Override
    @Cacheable(value = "allHashtags", key = "'all'", unless = "#result == null")
    public List<Hashtag> getAllHashtags() {
        return hashtagRepository.findAll();
    }
}
