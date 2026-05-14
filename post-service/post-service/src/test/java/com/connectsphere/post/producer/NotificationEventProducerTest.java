package com.connectsphere.post.producer;

import static org.mockito.Mockito.*;

import com.connectsphere.post.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NotificationEventProducerTest — verifies post-service publishes events to RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationEventProducer notificationEventProducer;

    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationEventProducer, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(notificationEventProducer, "routingKey", "notification.routing.key");

        testEvent = new NotificationEvent();
        testEvent.setRecipientEmail("user@gmail.com");
        testEvent.setType("MENTION");
        testEvent.setMessage("Someone mentioned you in a post");
    }

    @Test
    @DisplayName("publish — should call rabbitTemplate.convertAndSend with correct params")
    void publish_CallsRabbitTemplate() {
        notificationEventProducer.publish(testEvent);

        verify(rabbitTemplate).convertAndSend("notification.exchange", "notification.routing.key", testEvent);
    }

    @Test
    @DisplayName("publish — should call convertAndSend exactly once per event")
    void publish_CalledExactlyOnce() {
        notificationEventProducer.publish(testEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }
}
