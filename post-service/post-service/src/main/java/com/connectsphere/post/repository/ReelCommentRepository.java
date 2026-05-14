package com.connectsphere.post.repository;

import com.connectsphere.post.entity.ReelComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReelComment} entities.
 * <p>
 * Supports querying chronological comments on specific short-form videos.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ReelCommentRepository {
 *         +findByReelIdOrderByCreatedAtAsc()
 *         +deleteByReelId()
 *     }
 * </pre>
 */
@Repository
public interface ReelCommentRepository extends JpaRepository<ReelComment, Long> {
    List<ReelComment> findByReelIdOrderByCreatedAtAsc(Long reelId);
    long countByReelId(Long reelId);
    void deleteByReelId(Long reelId);
}
