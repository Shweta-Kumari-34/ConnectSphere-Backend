package com.connectsphere.follow.dto;

import java.time.LocalDateTime;

// RabbitMQ event payload used to notify notification-service/email-service.
public class NotificationEvent {
    // Target user who receives notification.
    private String recipientEmail;
    // User who performed the action.
    private String actorEmail;
    // Event category, e.g., FOLLOW.
    private String type;
    // Optional target entity id.
    private Long targetId;
    // Client navigation path for this notification.
    private String deepLinkUrl;
    // Human-readable message.
    private String message;
    // Delivery priority.
    private String priority;
    // Event creation timestamp.
    private LocalDateTime createdAt;

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
