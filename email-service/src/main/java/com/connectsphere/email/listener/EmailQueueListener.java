package com.connectsphere.email.listener;

import com.connectsphere.email.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * <h1>EmailQueueListener</h1>
 * <p>Asynchronous message consumer that listens to RabbitMQ queues for notification events 
 * and dispatches them as actual emails via SMTP.</p>
 * 
 * <h2>Email Dispatch Flow:</h2>
 * <pre>
 * graph TD
 *     A[RabbitMQ Exchange] -->|Route| B(email.queue)
 *     B -->|Consume| C[EmailQueueListener]
 *     C -->|Construct| D[SimpleMailMessage]
 *     D --> E{JavaMailSender}
 *     E -- Success --> F[SMTP Server]
 *     E -- Failure --> G[Log Fallback]
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Asynchronous Consumption:</b> Prevents slow SMTP servers from blocking upstream services.</li>
 *     <li><b>Resilience:</b> Gracefully falls back to logging if the SMTP connection fails.</li>
 *     <li><b>Dynamic Content:</b> Builds email subjects and bodies dynamically from the {@link NotificationEvent} payload.</li>
 * </ul>
 */
@Component
public class EmailQueueListener {
    private static final Logger log = LoggerFactory.getLogger(EmailQueueListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.notification.email-from:notifications@connectsphere.local}")
    private String emailFrom;

    public EmailQueueListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "${app.notification.email-queue:email.queue}")
    public void consume(NotificationEvent event) {
        String to = event.getRecipientEmail();
        if (to == null || to.isBlank()) {
            log.warn("Email event without recipientEmail: {}", event);
            return;
        }
        String subject = "[ConnectSphere] " + (event.getType() == null ? "Notification" : event.getType());
        String body = event.getMessage() + "\n\nLink: " + (event.getDeepLinkUrl() == null ? "" : event.getDeepLinkUrl());
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(emailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent email to {} subject={}", to, subject);
        } catch (Exception ex) {
            log.warn("Failed to send email to {} — logging instead: {}", to, ex.getMessage());
            log.info("Email content => to={}, subject={}, body={}", to, subject, body);
        }
    }
}
