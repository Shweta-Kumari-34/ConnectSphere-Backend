package com.connectsphere.notification.messaging;

/**
 * Centralized registry of RabbitMQ routing keys used by the Notification Service.
 * <p>
 * Prevents magic strings when routing messages to specific queues (e.g., standard
 * notifications vs high-priority emails).
 * </p>
 *
 * <h3>Routing Architecture</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Exchange] -->|notification.created| B[Notification Queue];
 *     A -->|email.highpriority| C[Email Queue];
 * </pre>
 */
public final class NotificationRoutingKeys {
    public static final String CREATED = "notification.created";
    public static final String EMAIL_HIGH_PRIORITY = "email.highpriority";

    private NotificationRoutingKeys() {}
}
