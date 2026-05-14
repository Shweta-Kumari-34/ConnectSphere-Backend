package com.connectsphere.media.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * <h1>Media Entity</h1>
 * <p>Represents persistent media assets (images, videos) associated primarily with user posts.</p>
 * 
 * <h2>Storage Lifecycle:</h2>
 * <pre>
 * graph LR
 *     A[Upload] --> B{Media Entity}
 *     B --> C[Post ID Association]
 *     B --> D[File Path / CDN URL]
 *     B --> E[MimeType / Size Validation]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Soft Deletion:</b> Uses an {@code isDeleted} flag to hide assets without destroying records immediately.</li>
 *     <li><b>Metadata Tracking:</b> Tracks exact file sizes ({@code sizeKb}) and Mime types for efficient frontend rendering.</li>
 * </ul>
 */
@Entity
@Table(name = "media")
public class Media {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String userEmail;
    private Long postId;
    @Column(nullable = false) private String mediaUrl;
    private String mediaType; // IMAGE, VIDEO
    private String mimeType; // image/jpeg, video/mp4, etc.
    private long sizeKb;
    private boolean isDeleted;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public Media() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSizeKb() { return sizeKb; }
    public void setSizeKb(long sizeKb) { this.sizeKb = sizeKb; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
