package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Reel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Reel} entities.
 * <p>
 * Separated from standard posts to provide highly-optimized video feed lookups
 * while respecting the video's privacy settings.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ReelRepository {
 *         +findPublicReelsExcludingUser()
 *         +findByUserEmailOrderByCreatedAtDesc()
 *     }
 * </pre>
 */
@Repository
public interface ReelRepository extends JpaRepository<Reel, Long> {
    
    @Query("SELECT r FROM Reel r WHERE r.visibility = 'PUBLIC' AND r.userEmail <> :userEmail ORDER BY r.createdAt DESC")
    List<Reel> findPublicReelsExcludingUser(@Param("userEmail") String userEmail);

    List<Reel> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
