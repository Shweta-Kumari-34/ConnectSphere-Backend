package com.connectsphere.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class RabbitConfig {

    @Value("${app.notification.exchange}") private String exchangeName;
    @Value("${app.notification.queue}") private String notificationQueueName;
    @Value("${app.notification.routing-key}") private String notificationRoutingKey;
    @Value("${app.notification.email-queue}") private String emailQueueName;
    @Value("${app.notification.email-routing-key}") private String emailRoutingKey;
    @Value("${app.notification.dead-letter-queue}") private String deadLetterQueueName;

    @Bean
    public TopicExchange notificationExchange() { return new TopicExchange(exchangeName, true, false); }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueueName)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", exchangeName,
                        "x-dead-letter-routing-key", deadLetterQueueName
                ))
                .build();
    }

    @Bean
    public Queue emailQueue() { return QueueBuilder.durable(emailQueueName).build(); }

    @Bean
    public Queue deadLetterQueue() { return QueueBuilder.durable(deadLetterQueueName).build(); }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(notificationRoutingKey);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(emailRoutingKey);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(notificationExchange).with(deadLetterQueueName);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setRetryTemplate(retryTemplate());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOff);
        return retryTemplate;
    }
}
