package com.connectsphere.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for managing existing subscriptions.
 * <p>
 * Allows toggling auto-renew or upgrading plans.
 * </p>
 *
 * <h3>Subscription Action Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class SubscriptionActionRequestDto {
 *         +boolean autoRenew
 *         +String targetPlanCode
 *     }
 * </pre>
 */
@Data
@NoArgsConstructor
public class SubscriptionActionRequestDto {
    private boolean autoRenew;
    private String targetPlanCode;
    private String theme;
}
