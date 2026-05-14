package com.connectsphere.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a user's request for a verification badge.
 * <p>
 * Tracks the entire lifecycle of a verification request, from initial submission
 * through admin review, payment, and final badge activation.
 * </p>
 *
 * <h3>Verification Lifecycle</h3>
 * <pre class="mermaid">
 * stateDiagram-v2
 *     [*] --> DRAFT
 *     DRAFT --> SUBMITTED : User Applies
 *     SUBMITTED --> UNDER_REVIEW : Admin Picks Up
 *     UNDER_REVIEW --> APPROVED : Admin Approves
 *     UNDER_REVIEW --> REJECTED : Admin Rejects
 *     APPROVED --> PAYMENT_PENDING : Awaiting Fee
 *     PAYMENT_PENDING --> BADGE_ACTIVATED : Paid
 *     BADGE_ACTIVATED --> [*]
 *     REJECTED --> [*]
 * </pre>
 */
@Entity
@Table(name = "verification_requests")
@Getter
@Setter
@NoArgsConstructor
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String usernameSnapshot;

    private String fullNameSnapshot;
    private String profilePicUrlSnapshot;
    private String reason;

    private String documentUrl;
    private String selfieUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    private String rejectionReason;
    private String adminNote;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime submittedAt;

    public enum VerificationStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        PAYMENT_PENDING,
        BADGE_ACTIVATED
    }
}
