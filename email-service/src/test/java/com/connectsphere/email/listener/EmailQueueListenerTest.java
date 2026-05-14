package com.connectsphere.email.listener;

import static org.mockito.Mockito.*;

import com.connectsphere.email.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmailQueueListenerTest — Unit tests for the RabbitMQ email consumer.
 * Verifies that emails are sent correctly and edge cases (null/blank recipient) are handled.
 */
@ExtendWith(MockitoExtension.class)
class EmailQueueListenerTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailQueueListener emailQueueListener;

    @BeforeEach
    void setUp() {
        // Inject the @Value field manually since we're not using Spring context
        ReflectionTestUtils.setField(emailQueueListener, "emailFrom", "notifications@connectsphere.local");
    }

    @Test
    @DisplayName("consume — should send email for valid notification event")
    void consume_ValidEvent_SendsEmail() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("user@gmail.com");
        event.setType("LIKE");
        event.setMessage("Someone liked your post");
        event.setDeepLinkUrl("https://connectsphere.com/posts/1");

        emailQueueListener.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("notifications@connectsphere.local", sent.getFrom());
        assertEquals("user@gmail.com", sent.getTo()[0]);
        assertTrue(sent.getSubject().contains("LIKE"));
        assertTrue(sent.getText().contains("Someone liked your post"));
        assertTrue(sent.getText().contains("https://connectsphere.com/posts/1"));
    }

    @Test
    @DisplayName("consume — should send email with null deepLinkUrl gracefully")
    void consume_NullDeepLinkUrl() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("user@gmail.com");
        event.setType("FOLLOW");
        event.setMessage("Someone followed you");
        event.setDeepLinkUrl(null);

        emailQueueListener.consume(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("consume — should skip sending when recipientEmail is null")
    void consume_NullRecipient_Skips() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail(null);
        event.setType("COMMENT");
        event.setMessage("Someone commented");

        emailQueueListener.consume(event);

        // mailSender.send() should NOT be called for null recipient
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("consume — should skip sending when recipientEmail is blank")
    void consume_BlankRecipient_Skips() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("  ");
        event.setType("LIKE");
        event.setMessage("Someone liked your post");

        emailQueueListener.consume(event);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("consume — should handle null type gracefully with fallback subject")
    void consume_NullType_UsesFallbackSubject() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("user@gmail.com");
        event.setType(null);
        event.setMessage("You have a new notification");
        event.setDeepLinkUrl(null);

        emailQueueListener.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Notification"));
    }

    @Test
    @DisplayName("consume — should log and NOT throw when mailSender fails")
    void consume_MailSenderFails_DoesNotThrow() {
        NotificationEvent event = new NotificationEvent();
        event.setRecipientEmail("user@gmail.com");
        event.setType("LIKE");
        event.setMessage("Someone liked your post");
        event.setDeepLinkUrl(null);

        doThrow(new RuntimeException("SMTP connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should NOT throw — exception is caught and logged
        assertDoesNotThrow(() -> emailQueueListener.consume(event));
    }
}
