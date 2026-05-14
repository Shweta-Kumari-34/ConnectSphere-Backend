package com.connectsphere.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a One-Time Password (OTP) used for authentication flows.
 * <p>
 * This entity tracks the OTP code, the associated email, its purpose (e.g., SIGNUP, RESET),
 * expiration time, and usage status. It ensures that OTPs are used securely and only once.
 * </p>
 *
 * <h3>Entity Relationship</h3>
 * <pre class="mermaid">
 * erDiagram
 *     OtpEntity {
 *         Long id PK
 *         String email
 *         String otpCode
 *         String purpose
 *         LocalDateTime expiresAt
 *         int retryCount
 *         boolean used
 *     }
 * </pre>
 */
@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private String purpose; // SIGNUP, LOGIN, RESET

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private int retryCount = 0;

    private boolean used = false;
}
