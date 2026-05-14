package com.connectsphere.media.dto;

import java.time.LocalDateTime;

/**
 * Standardized data transfer object for inter-service notifications via RabbitMQ.
 * <p>
 * This payload carries all necessary contextual data so the Notification Service
 * can properly format and dispatch real-time alerts or emails to the recipient.
 * Used in the Media Service primarily when story/reel uploads trigger alerts.
 * </p>
 *
 * <h3>Notification Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class NotificationEvent {
 *         +String recipientEmail
 *         +String actorEmail
 *         +String type
 *         +Long targetId
 *         +String deepLinkUrl
 *         +String message
 *     }
 *     class NotificationEventProducer {
 *         +publish(NotificationEvent)
 *     }
 *     NotificationEventProducer --> NotificationEvent : Serializes
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
