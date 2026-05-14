package com.connectsphere.search.service;

import com.connectsphere.search.entity.Hashtag;
import java.util.List;

/**
 * <h1>SearchService Interface</h1>
 * <p>Powers the discovery engine of ConnectSphere by indexing post content and managing trending topics via hashtags.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>Indexing:</b> Parsing content to extract and persist hashtags linked to specific posts.</li>
 *     <li><b>Discovery:</b> Allowing users to query posts via keywords or exact hashtag matches.</li>
 *     <li><b>Trending Algorithms:</b> Aggregating hashtag frequency to generate real-time trending lists.</li>
 * </ul>
 * 
 * <h2>Search Indexing Flow:</h2>
 * <pre>
 * graph TD
 *     A[Post Created] -->|Async Event| B[SearchService]
 *     B -->|Regex Extract| C{Hashtags Found?}
 *     C -- Yes --> D[Index Hashtag to Post ID]
 *     C -- No --> E[End]
 *     D --> F[(Search DB)]
 *     G[User Search] --> F
 *     F --> H[Return Post IDs]
 * </pre>
 */
public interface SearchService {

    void indexPost(Long postId, String content);

    void removePostIndex(Long postId);

    Hashtag indexHashtag(String tag, Long postId);

    List<Hashtag> searchByTag(String tag);

    List<Long> searchPosts(String keyword);

    List<String> searchUsers(String keyword);

    List<Hashtag> getHashtagsForPost(Long postId);

    List<Object[]> getTrendingHashtags(int limit);

    List<Long> getPostsByHashtag(String tag);

    List<Hashtag> searchHashtags(String keyword);

    long getHashtagCount(String tag);

    List<Hashtag> getAllHashtags();
}
