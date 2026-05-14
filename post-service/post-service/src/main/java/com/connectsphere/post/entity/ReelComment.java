package com.connectsphere.post.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a comment on a specific Reel.
 * <p>
 * Allows users to interact directly with short-form videos.
 * </p>
 *
 * <h3>Reel Comment Structure</h3>
 * <pre class="mermaid">
 * erDiagram
 *     REEL_COMMENT {
 *         Long id PK
 *         Long reelId FK
 *         String userEmail
 *         String content
 *     }
 * </pre>
 */
@Entity
@Table(name = "reel_comments")
public class ReelComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reelId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReelId() { return reelId; }
    public void setReelId(Long reelId) { this.reelId = reelId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
