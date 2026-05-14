package com.connectsphere.media.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * <h1>Story Entity</h1>
 * <p>Represents a 24-hour ephemeral media status update posted by a user.</p>
 * 
 * <h2>Ephemeral Lifecycle:</h2>
 * <pre>
 * graph TD
 *     A[User Uploads Story] --> B[PrePersist Hook]
 *     B -->|Set +24h Expiry| C[Story Active]
 *     C --> D{Scheduler / Time passes}
 *     D -->|24h Reached| E[Story Expires]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Automatic Expiration:</b> Uses JPA {@code @PrePersist} to automatically set a 24-hour expiration timestamp upon creation.</li>
 *     <li><b>Engagement Tracking:</b> Maintains an aggregate count of unique views.</li>
 * </ul>
 */
@Entity
@Table(name = "stories")
public class Story {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String userEmail;
    @Column(nullable = false) private String mediaUrl;
    private String caption;
    private String mediaType; // IMAGE, VIDEO
    private int viewsCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(24);
        this.active = true;
        this.viewsCount = 0;
    }

    public Story() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
