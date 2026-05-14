package com.connectsphere.post.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RabbitMQ message broker integration.
 * <p>
 * Sets up JSON serialization for internal notification events dispatched
 * when users interact with posts or reels.
 * </p>
 *
 * <h3>Messaging Configuration</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Event Producer] -->|JSON| B[RabbitTemplate];
 *     B --> C[RabbitMQ Exchange];
 * </pre>
 */
@Configuration
public class RabbitMqConfig {
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }
}
