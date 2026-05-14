package com.connectsphere.like.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h1>RabbitMqConfig</h1>
 * <p>Configures RabbitMQ integration for asynchronous message publishing, enabling decoupled 
 * notification dispatch from the core engagement logic.</p>
 * 
 * <h2>Message Publishing Flow:</h2>
 * <pre>
 * graph LR
 *     A[LikeServiceImpl] -->|Create Event| B[RabbitTemplate]
 *     B -->|Serialize JSON| C[Jackson2JsonMessageConverter]
 *     C -->|Publish| D[RabbitMQ Exchange]
 *     D -->|Route| E[Notification Queue]
 * </pre>
 * 
 * <h2>Configuration Details:</h2>
 * <ul>
 *     <li><b>Message Conversion:</b> Injects {@link Jackson2JsonMessageConverter} to automatically translate Java objects to JSON payloads.</li>
 *     <li><b>Template Configuration:</b> Binds the converter to the {@link RabbitTemplate} for seamless producer usage.</li>
 * </ul>
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
