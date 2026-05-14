package com.connectsphere.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the response to a payment intent or confirmation.
 * <p>
 * Returns provider-specific details (like Razorpay order IDs or UPI QR strings)
 * needed by the frontend to render the payment gateway.
 * </p>
 *
 * <h3>Response Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PaymentResponseDto {
 *         +Long paymentId
 *         +String status
 *         +String razorpayOrderId
 *         +String upiQrPayload
 *     }
 * </pre>
 */
@Data
@NoArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;
    private String message;
    private BigDecimal amount;
    private String currency;
    private String planCode;
    private String status;
    private String transactionId;
    private String paymentProvider;
    private String paymentMethod;
    private String providerOrderId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private String receiptNumber;
    private String upiQrPayload;
    private String upiQrImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expiresAt;
}
