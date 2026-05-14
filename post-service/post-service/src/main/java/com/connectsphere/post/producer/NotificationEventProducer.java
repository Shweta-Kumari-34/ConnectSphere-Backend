package com.connectsphere.post.producer;

import com.connectsphere.post.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Asynchronous producer for routing social interactions to the Notification Service.
 * <p>
 * Captures events such as mentions and reel comments and dispatches them 
 * via RabbitMQ. The operation is fail-safe to prevent post interactions 
 * from failing if the broker is down.
 * </p>
 *
 * <h3>Producer Workflow</h3>
 * <pre class="mermaid">
 * sequenceDiagram
 *     participant S as PostService
 *     participant P as NotificationEventProducer
 *     participant R as RabbitMQ Exchange
 *     
 *     S->>P: publish(NotificationEvent)
 *     alt RabbitMQ Active
 *         P->>R: convertAndSend()
 *     else Broker Down
 *         P-->>S: Catch & Log Warn
 *     end
 * </pre>
 */
@Component
public class NotificationEventProducer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);
    private final RabbitTemplate rabbitTemplate;
    @Value("${connectsphere.notification.exchange}") private String exchange;
    @Value("${connectsphere.notification.routing-key}") private String routingKey;

    public NotificationEventProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            log.warn("Notification publish skipped (rabbitmq unavailable): {}", ex.getMessage());
        }
    }
}
