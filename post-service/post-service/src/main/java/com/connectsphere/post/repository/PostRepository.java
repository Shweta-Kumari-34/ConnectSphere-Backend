package com.connectsphere.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.connectsphere.post.entity.Post;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Post} entities.
 * <p>
 * Supports querying the global feed, user-specific walls, and keyword search.
 * Excludes soft-deleted posts by default.
 * Matches case study §4.2 class diagram.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PostRepository {
 *         +findFeedByUserEmails()
 *         +searchByContent()
 *     }
 * </pre>
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserEmail(String userEmail);

    List<Post> findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(String userEmail);

    List<Post> findByVisibility(String visibility);

    java.util.Optional<Post> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT p FROM Post p WHERE p.userEmail IN ?1 AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Post> findFeedByUserEmails(List<String> userEmails);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND (p.title LIKE %?1% OR p.content LIKE %?1%)")
    List<Post> searchByContent(String keyword);

    long countByUserEmailAndIsDeletedFalse(String userEmail);

    List<Post> findAllByIsDeletedFalseOrderByCreatedAtDesc();
}
