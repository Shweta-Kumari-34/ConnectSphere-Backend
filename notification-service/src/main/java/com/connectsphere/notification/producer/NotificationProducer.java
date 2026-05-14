package com.connectsphere.notification.producer;

import com.connectsphere.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Asynchronous producer for publishing internal notification events to RabbitMQ.
 * <p>
 * Handles dispatching high-priority emails (e.g., OTP, password resets) and 
 * standard notification events back into the message broker if re-routing is needed.
 * </p>
 *
 * <h3>Producer Workflow</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[NotificationService] -->|publish| B(RabbitTemplate);
 *     B -->|emailRoutingKey| C((Email Queue));
 *     B -->|notificationRoutingKey| D((Notification Queue));
 * </pre>
 */
@Component
public class NotificationProducer {
    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.notification.exchange}") private String exchange;
    @Value("${app.notification.email-routing-key}") private String emailRoutingKey;
    @Value("${app.notification.routing-key}") private String notificationRoutingKey;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishHighPriorityEmail(NotificationEvent event) {
        rabbitTemplate.convertAndSend(exchange, emailRoutingKey, event);
        log.info("Published high priority email event type={} recipient={}", event.getType(), event.getRecipientEmail());
    }

    public void publishNotificationEvent(NotificationEvent event) {
        rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
        log.info("Published notification event type={} recipient={}", event.getType(), event.getRecipientEmail());
    }
}
