package com.connectsphere.email.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a notification event shared between services.
 * Used as RabbitMQ payload for email/notification processing.
 */
public class NotificationEvent {
    // User receiving the notification.
    private Long recipientId;
    // User who triggered the action.
    private Long actorId;
    // Recipient email for delivery/routing.
    private String recipientEmail;
    // Actor email shown in message context.
    private String actorEmail;
    // Event type (LIKE, COMMENT, FOLLOW, SYSTEM, etc.).
    private String type;
    // Target entity id (post/comment/story/reel id).
    private Long targetId;
    // Frontend deep link to the affected content.
    private String deepLinkUrl;
    // Human-readable message body.
    private String message;
    // Priority level (HIGH, NORMAL, LOW).
    private String priority;
    // Event creation time.
    private LocalDateTime createdAt;

    // Manual accessors are kept for reliable JSON serialization/deserialization across services.
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getDeepLinkUrl() { return deepLinkUrl; }
    public void setDeepLinkUrl(String deepLinkUrl) { this.deepLinkUrl = deepLinkUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
