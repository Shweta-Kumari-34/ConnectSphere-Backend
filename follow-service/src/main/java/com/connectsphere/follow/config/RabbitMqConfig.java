package com.connectsphere.follow.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ message broker integration.
 * <p>
 * This configuration defines the necessary beans for the Follow Service to produce
 * asynchronous events (such as follow notifications) to RabbitMQ. It sets up the {@link RabbitTemplate}
 * and ensures messages are serialized as JSON.
 * </p>
 *
 * <h3>Messaging Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[FollowService] -->|Creates Follow Event| B(RabbitTemplate);
 *     B -->|JSON Serialized| C((RabbitMQ Exchange));
 *     C --> D[Notification Service Queue];
 * </pre>
 */
@Configuration
public class RabbitMqConfig {
    /**
     * Configures the Jackson message converter for serializing AMQP messages to JSON.
     * 
     * @return the configured {@link Jackson2JsonMessageConverter}
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the RabbitTemplate used for sending messages.
     * <p>
     * Associates the JSON message converter with the template to ensure all outbound
     * events are properly formatted.
     * </p>
     * 
     * @param connectionFactory the RabbitMQ connection factory
     * @return the configured {@link RabbitTemplate}
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }
}
