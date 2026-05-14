package com.connectsphere.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for handling asynchronous payment provider webhooks.
 * <p>
 * Used to update transaction statuses when payments succeed or fail in the background.
 * </p>
 *
 * <h3>Webhook Flow</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PaymentWebhookRequestDto {
 *         +String eventType
 *         +String providerPaymentId
 *         +String status
 *     }
 * </pre>
 */
@Data
@NoArgsConstructor
public class PaymentWebhookRequestDto {

    private Long paymentId;
    private String transactionId;
    private String eventType;
    private String providerPaymentId;
    private String status;
    private Long timestamp;
}
