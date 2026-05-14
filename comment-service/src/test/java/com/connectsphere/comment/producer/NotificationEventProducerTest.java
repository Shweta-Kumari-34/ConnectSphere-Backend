package com.connectsphere.comment.producer;

import static org.mockito.Mockito.*;

import com.connectsphere.comment.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationEventProducer notificationEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationEventProducer, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(notificationEventProducer, "routingKey", "notification.routing.key");
    }

    @Test
    @DisplayName("publish — should call rabbitTemplate.convertAndSend for comment event")
    void publish_CallsRabbitTemplate() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("postOwner@gmail.com");
        event.setType("COMMENT");
        event.setMessage("Someone commented on your post");

        notificationEventProducer.publish(event);

        verify(rabbitTemplate).convertAndSend("notification.exchange", "notification.routing.key", event);
    }
}
