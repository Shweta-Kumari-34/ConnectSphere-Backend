package com.connectsphere.auth.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for subscription activation requests.
 * <p>
 * This object is typically used in service-to-service communication
 * (e.g., from a payment or subscription service) to update a user's 
 * premium status or verification badge in the Auth Service.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PaymentService
 *     class InternalSubscriptionActivationRequestDto {
 *         +String userEmail
 *         +String planCode
 *         +boolean active
 *         +LocalDateTime expiresAt
 *     }
 *     class AuthService
 *     PaymentService --> InternalSubscriptionActivationRequestDto : Creates
 *     InternalSubscriptionActivationRequestDto --> AuthService : Consumes
 * </pre>
 */
@Data
@NoArgsConstructor
public class InternalSubscriptionActivationRequestDto {

    @NotBlank(message = "User email is required")
    private String userEmail;

    @NotBlank(message = "Plan code is required")
    private String planCode; // VERIFIED_BADGE or PREMIUM_MEMBERSHIP

    private boolean active;
    private LocalDateTime expiresAt;
    private boolean autoRenew;
    private String theme;
    private String sourcePaymentReference;
}
