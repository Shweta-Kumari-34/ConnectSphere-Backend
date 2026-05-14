package com.connectsphere.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for confirming a processed payment.
 * <p>
 * Contains the provider's signature and reference ID required to finalize
 * the transaction securely.
 * </p>
 *
 * <h3>Verification Flow</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PaymentConfirmRequestDto {
 *         +Long paymentId
 *         +String providerPaymentId
 *         +String providerSignature
 *     }
 * </pre>
 */
@Data
@NoArgsConstructor
public class PaymentConfirmRequestDto {

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    private String providerPaymentId;
    private String providerSignature;
    private String upiReference;
    private String webhookEventId;
    private boolean success = true;
}
