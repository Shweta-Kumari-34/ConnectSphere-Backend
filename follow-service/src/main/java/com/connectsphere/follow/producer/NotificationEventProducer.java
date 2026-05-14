package com.connectsphere.follow.producer;

import com.connectsphere.follow.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Asynchronous producer for publishing follow-related events to RabbitMQ.
 * <p>
 * When a user follows another user, this producer formats the event into a
 * {@link NotificationEvent} and dispatches it. The operation is designed to be
 * non-blocking and fail-safe; if RabbitMQ is down, the failure is logged but
 * does not prevent the follow action from succeeding.
 * </p>
 *
 * <h3>Producer Workflow</h3>
 * <pre class="mermaid">
 * sequenceDiagram
 *     participant S as FollowService
 *     participant P as NotificationEventProducer
 *     participant R as RabbitMQ Exchange
 *     
 *     S->>P: publish(NotificationEvent)
 *     alt RabbitMQ is Available
 *         P->>R: convertAndSend()
 *     else RabbitMQ is Down
 *         P-->>S: Catch Exception & Log Warn (No Crash)
 *     end
 * </pre>
 */
@Component
public class NotificationEventProducer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);
    private final RabbitTemplate rabbitTemplate;
    @Value("${app.notification.exchange}") private String exchange;
    @Value("${app.notification.routing-key}") private String routingKey;

    public NotificationEventProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    /**
     * Publishes a notification event to the configured RabbitMQ exchange.
     * 
     * @param event the populated notification payload to send
     */
    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            // Non-blocking behavior: follow action should still succeed if broker is down.
            log.warn("Notification publish skipped (rabbitmq unavailable): {}", ex.getMessage());
        }
    }
}
