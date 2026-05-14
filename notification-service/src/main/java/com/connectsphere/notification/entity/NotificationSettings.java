package com.connectsphere.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a user's notification preferences.
 * <p>
 * Allows users to globally mute notifications, disable emails, or disable
 * push notifications.
 * </p>
 *
 * <h3>Settings Flow</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class NotificationSettings {
 *         +String recipientEmail
 *         +boolean emailEnabled
 *         +boolean pushEnabled
 *         +boolean muted
 *         +LocalDateTime mutedUntil
 *     }
 * </pre>
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String recipientEmail;
    private boolean emailEnabled = true;
    private boolean pushEnabled = true;
    private boolean muted = false;
    private LocalDateTime mutedUntil;

    public NotificationSettings() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public LocalDateTime getMutedUntil() { return mutedUntil; }
    public void setMutedUntil(LocalDateTime mutedUntil) { this.mutedUntil = mutedUntil; }
}
