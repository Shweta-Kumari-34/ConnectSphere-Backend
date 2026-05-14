package com.connectsphere.notification.consumer;

import com.connectsphere.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(EmailQueueConsumer.class);

    @RabbitListener(queues = "${app.notification.email-queue}")
    public void consume(NotificationEvent event) {
        // Placeholder until dedicated email-service is connected.
        log.info("Email queue consumed: recipient={}, type={}, priority={}",
                event.getRecipientEmail(), event.getType(), event.getPriority());
    }
}
