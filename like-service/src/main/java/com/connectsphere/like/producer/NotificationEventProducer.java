package com.connectsphere.like.producer;

import com.connectsphere.like.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h1>NotificationEventProducer</h1>
 * <p>Component responsible for publishing engagement events (like/reaction) to the RabbitMQ 
 * exchange for asynchronous processing by the notification-service.</p>
 * 
 * <h2>Message Dispatch Flow:</h2>
 * <pre>
 * graph TD
 *     A[Like Service] -->|Create Event| B[NotificationEventProducer]
 *     B -->|Serialize & Route| C{RabbitTemplate}
 *     C -- Success --> D[(RabbitMQ Exchange)]
 *     C -- Fail (Broker Down) --> E[Log Warning & Continue]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Fault Tolerance:</b> Wraps AMQP calls in a try-catch to prevent broker failures from breaking the core user engagement flow.</li>
 *     <li><b>Dynamic Routing:</b> Injects exchange and routing key configurations directly from application properties.</li>
 * </ul>
 */
@Component
public class NotificationEventProducer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);
    private final RabbitTemplate rabbitTemplate;
    @Value("${app.notification.exchange}") private String exchange;
    @Value("${app.notification.routing-key}") private String routingKey;

    public NotificationEventProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            log.warn("Notification publish skipped (rabbitmq unavailable): {}", ex.getMessage());
        }
    }
}
