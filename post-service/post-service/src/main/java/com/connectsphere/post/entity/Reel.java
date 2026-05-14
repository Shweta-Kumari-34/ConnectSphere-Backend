package com.connectsphere.post.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a short-form video (Reel).
 * <p>
 * Separated from standard posts to optimize video-specific queries and feeds.
 * </p>
 *
 * <h3>Reel Entity Structure</h3>
 * <pre class="mermaid">
 * erDiagram
 *     REEL {
 *         Long id PK
 *         String userEmail
 *         String videoUrl
 *         String caption
 *         int likesCount
 *     }
 * </pre>
 */
@Entity
@Table(name = "reels")
public class Reel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    @JsonProperty("mediaUrl")
    private String videoUrl;

    private String caption;

    @Column(nullable = false)
    private String visibility = "PUBLIC"; // PUBLIC, PRIVATE

    private int likesCount = 0;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Reel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
