package com.connectsphere.payment.entity;

import java.math.BigDecimal;
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
 * Core entity representing a financial transaction.
 * <p>
 * Tracks the lifecycle of a payment from PENDING to SUCCESS/FAILED.
 * </p>
 *
 * <h3>Payment Entity</h3>
 * <pre class="mermaid">
 * erDiagram
 *     PAYMENT {
 *         Long paymentId PK
 *         String userEmail
 *         BigDecimal amount
 *         PaymentStatus status
 *     }
 * </pre>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String description;
    private String paymentMethod;
    private String paymentProvider;
    private String providerOrderId;
    private String providerPaymentId;
    private String planCode;
    private String transactionId;
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Lob
    private String metadataJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expiresAt;

    public enum PaymentStatus {
        PENDING,
        CONFIRMED,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
