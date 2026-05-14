package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Hashtag} entities.
 * <p>
 * Provides highly optimized queries for counting tag occurrences and
 * finding the top trending hashtags via grouping.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class HashtagRepository {
 *         +findByTagIgnoreCase()
 *         +findTrendingHashtags()
 *     }
 * </pre>
 */
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    List<Hashtag> findByPostId(Long postId);

    List<Hashtag> findByTagIgnoreCase(String tag);

    // Partial match search (LIKE %tag%)
    List<Hashtag> findByTagContainingIgnoreCase(String tag);

    void deleteByPostId(Long postId);

    long countByTagIgnoreCase(String tag);

    @Query("SELECT h.tag, COUNT(h) as cnt FROM Hashtag h GROUP BY h.tag ORDER BY cnt DESC")
    List<Object[]> findTrendingHashtags();
}
