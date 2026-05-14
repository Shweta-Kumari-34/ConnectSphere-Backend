package com.connectsphere.post.dto;

import java.time.LocalDateTime;

/**
 * DTO for dispatching notification events via RabbitMQ.
 * <p>
 * Carries the recipient email, deep-link URL, and priority necessary
 * to generate real-time alerts.
 * </p>
 *
 * <h3>Notification Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class NotificationEvent {
 *         +String recipientEmail
 *         +String deepLinkUrl
 *         +String type
 *     }
 * </pre>
 */
public class NotificationEvent {
    private String recipientEmail;
    private String actorEmail;
    private String type;
    private Long targetId;
    private String deepLinkUrl;
    private String message;
    private String priority;
    private String metadata;
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
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
