package com.connectsphere.payment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an active user subscription.
 * <p>
 * Defines the user's premium tier (e.g., VERIFIED_BADGE, PREMIUM_MEMBERSHIP)
 * and its expiration logic.
 * </p>
 *
 * <h3>Subscription Entity</h3>
 * <pre class="mermaid">
 * erDiagram
 *     SUBSCRIPTION {
 *         Long id PK
 *         String userEmail
 *         String planCode
 *         SubscriptionStatus status
 *         LocalDate expiryDate
 *     }
 * </pre>
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private String planCode; // VERIFIED_BADGE | PREMIUM_MEMBERSHIP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private boolean autoRenew;
    private LocalDateTime lastRenewedAt;
    private LocalDateTime cancelledAt;
    private boolean renewalReminderSent;

    @Lob
    private String featureSnapshot;

    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        PENDING
    }
}
