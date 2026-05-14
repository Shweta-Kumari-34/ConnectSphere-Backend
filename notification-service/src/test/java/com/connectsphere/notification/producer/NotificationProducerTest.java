package com.connectsphere.notification.producer;

import static org.mockito.Mockito.*;

import com.connectsphere.notification.dto.NotificationEvent;
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
 * NotificationProducerTest — verifies RabbitMQ publishing for notification and email events.
 */
@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationProducer, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(notificationProducer, "emailRoutingKey", "email.routing.key");
        ReflectionTestUtils.setField(notificationProducer, "notificationRoutingKey", "notification.routing.key");

        testEvent = new NotificationEvent();
        testEvent.setRecipientEmail("user@gmail.com");
        testEvent.setType("LIKE");
        testEvent.setMessage("Someone liked your post");
    }

    @Test
    @DisplayName("publishHighPriorityEmail — should call rabbitTemplate with email routing key")
    void publishHighPriorityEmail_CallsRabbitTemplate() {
        notificationProducer.publishHighPriorityEmail(testEvent);

        verify(rabbitTemplate).convertAndSend("notification.exchange", "email.routing.key", testEvent);
    }

    @Test
    @DisplayName("publishNotificationEvent — should call rabbitTemplate with notification routing key")
    void publishNotificationEvent_CallsRabbitTemplate() {
        notificationProducer.publishNotificationEvent(testEvent);

        verify(rabbitTemplate).convertAndSend("notification.exchange", "notification.routing.key", testEvent);
    }
}
