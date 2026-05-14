package com.connectsphere.post.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Optional Kafka producer for dispatching global post creation events.
 * <p>
 * Utilized by downstream analytics or feed aggregation services if Kafka is active.
 * </p>
 *
 * <h3>Kafka Flow</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[PostService] -->|Create Post| B[PostEventProducer];
 *     B -->|Topic: post-events| C[(Kafka Cluster)];
 * </pre>
 */
//@Component
public class PostEventProducer {

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendPostCreatedEvent(String userEmail, String postTitle) {
        if (kafkaTemplate != null) {
            String message = String.format("{\"event\":\"POST_CREATED\",\"userEmail\":\"%s\",\"title\":\"%s\"}", userEmail, postTitle);
            try {
                kafkaTemplate.send("post-events", message);
                System.out.println("Kafka message sent: " + message);
            } catch (Exception e) {
                System.out.println("Kafka send failed: " + e.getMessage());
            }
        } else {
            System.out.println("Kafka disabled -> skipping event for: " + postTitle);
        }
    }
}
