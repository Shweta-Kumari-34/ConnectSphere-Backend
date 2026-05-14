package com.connectsphere.post.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Optional Kafka consumer for post-related events.
 * <p>
 * Conditionally initialized based on the availability of {@code KafkaTemplate}.
 * Used for subscribing to global feed aggregations if the full Kafka stack is deployed.
 * </p>
 *
 * <h3>Kafka Flow</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[(Kafka Cluster)] -->|Topic: post-events| B[PostEventConsumer];
 * </pre>
 */
//@Component
@ConditionalOnBean(KafkaTemplate.class)
public class PostEventConsumer {

    @KafkaListener(topics = "post-events", groupId = "post-service-group")
    public void consume(String message) {
        System.out.println("Received: " + message);
    }
}
