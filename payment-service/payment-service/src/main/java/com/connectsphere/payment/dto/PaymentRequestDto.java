package com.connectsphere.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating a new payment intent.
 * <p>
 * Specifies the plan (Premium/Verified) and the selected payment provider.
 * </p>
 *
 * <h3>Intent Flow</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PaymentRequestDto {
 *         +String planCode
 *         +String paymentProvider
 *         +BigDecimal amount
 *     }
 * </pre>
 */
@Data
@NoArgsConstructor
public class PaymentRequestDto {

    @NotBlank(message = "Plan code is required")
    private String planCode; // VERIFIED_BADGE | PREMIUM_MEMBERSHIP

    @NotBlank(message = "Payment provider is required")
    private String paymentProvider; // UPI_QR | RAZORPAY

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // UPI | CARD | WALLET

    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    private BigDecimal amount;

    private String description;
    private boolean autoRenew = true;
    private String theme;
}
