package com.connectsphere.search.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a hashtag extracted from a user's post.
 * <p>
 * Allows the search service to quickly index and query trending topics
 * without needing to perform expensive full-text scans on the original posts.
 * </p>
 *
 * <h3>Hashtag Entity Structure</h3>
 * <pre class="mermaid">
 * erDiagram
 *     HASHTAG {
 *         Long id PK
 *         String tag
 *         Long postId
 *     }
 * </pre>
 */
@Entity
@Table(name = "hashtags")
public class Hashtag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String tag; // e.g. "java"
    @Column(nullable = false) private Long postId;
    private LocalDateTime createdAt;

    public Hashtag() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTag() { return tag; } public void setTag(String tag) { this.tag = tag; }
    public Long getPostId() { return postId; } public void setPostId(Long postId) { this.postId = postId; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
