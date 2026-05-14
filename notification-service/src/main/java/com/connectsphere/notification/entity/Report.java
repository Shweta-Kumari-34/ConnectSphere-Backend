package com.connectsphere.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a user-submitted moderation report.
 * <p>
 * Reports can target Posts, Comments, or Accounts. This entity tracks the reason,
 * current status (PENDING, RESOLVED, DISMISSED), and administrative notes.
 * </p>
 *
 * <h3>Report Lifecycle</h3>
 * <pre class="mermaid">
 * stateDiagram-v2
 *     [*] --> PENDING : User Submits
 *     PENDING --> RESOLVED : Admin Acts
 *     PENDING --> DISMISSED : Admin Rejects
 *     RESOLVED --> [*]
 *     DISMISSED --> [*]
 * </pre>
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reporterEmail;

    @Column(nullable = false)
    private String targetType; // POST, COMMENT, ACCOUNT

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status; // PENDING, RESOLVED, DISMISSED

    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    public Report() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
