package com.connectsphere.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Core entity representing a user notification in the ConnectSphere platform.
 * <p>
 * This entity stores the metadata, deep link URL, and interaction context 
 * needed to render real-time and historical notifications to the user.
 * It includes special Jackson mappings to ensure seamless JSON serialization
 * for the Angular frontend's generic notification components.
 * </p>
 *
 * <h3>Entity Structure</h3>
 * <pre class="mermaid">
 * erDiagram
 *     NOTIFICATION {
 *         Long id PK
 *         String recipientEmail
 *         String senderEmail
 *         String type
 *         String message
 *         String deepLinkUrl
 *         boolean isRead
 *     }
 * </pre>
 */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recipientId;
    private Long actorId;
    @Column(nullable = false) private String recipientEmail;
    @Column(nullable = false) private String senderEmail;
    @Column(nullable = false) private String type;
    @Column(length = 500) private String message;
    private Long targetId;
    @Column(length = 500) private String deepLinkUrl;
    @Column(length = 30) private String priority;
    private Long referenceId; // postId, commentId, etc.
    private String referenceType;
    @Column(length = 500)
    private String actionUrl;
    @Column(length = 2000)
    private String metadata;
    private boolean isRead;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Notification() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDeepLinkUrl() { return deepLinkUrl; }
    public void setDeepLinkUrl(String deepLinkUrl) { this.deepLinkUrl = deepLinkUrl; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Convenience JSON aliases so the Angular notification dashboard can deep-link reliably
    // without requiring schema changes in the frontend models.
    @JsonProperty("postId")
    public Long getPostId() {
        if (isType("COMMENT") || isType("REPLY") || isType("COMMENT_LIKE")) {
            return null;
        }
        if (isType("STORY_LIKE") || (actionUrl != null && actionUrl.startsWith("/stories"))) {
            return null;
        }
        if (isType("REEL_LIKE") || isType("REEL_COMMENT") || (actionUrl != null && actionUrl.startsWith("/reels"))) {
            return null;
        }
        return targetId != null ? targetId : referenceId;
    }

    @JsonProperty("commentId")
    public Long getCommentId() {
        if (isType("COMMENT") || isType("REPLY") || isType("COMMENT_LIKE")) {
            return targetId != null ? targetId : referenceId;
        }
        return null;
    }

    @JsonProperty("storyId")
    public Long getStoryId() {
        if (isType("STORY_LIKE") || (actionUrl != null && actionUrl.startsWith("/stories"))) {
            return targetId != null ? targetId : referenceId;
        }
        return null;
    }

    @JsonProperty("reelId")
    public Long getReelId() {
        if (isType("REEL_LIKE") || isType("REEL_COMMENT") || (actionUrl != null && actionUrl.startsWith("/reels"))) {
            return targetId != null ? targetId : referenceId;
        }
        return null;
    }

    @JsonIgnore
    private boolean isType(String expected) {
        return type != null && type.equalsIgnoreCase(expected);
    }
}
