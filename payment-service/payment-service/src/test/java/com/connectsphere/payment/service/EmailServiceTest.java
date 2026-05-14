package com.connectsphere.payment.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private EmailService emailService;
    private JavaMailSender javaMailSender;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        emailService = new EmailService(javaMailSender);
        
        // Mock default configuration
        ReflectionTestUtils.setField(emailService, "demoEmailEnabled", true);
        ReflectionTestUtils.setField(emailService, "demoOutboxDir", tempDir.toString());
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@connectsphere.com");
        ReflectionTestUtils.setField(emailService, "smtpPassword", "secret");
    }

    @Test
    @DisplayName("SendEmail - should send SMTP email when configured")
    void sendEmail_SmtpSuccess() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = emailService.sendEmail("user@gmail.com", "Test Subject", "Test Body");

        assertTrue(result);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("SendEmail - should fallback to demo outbox when SMTP not configured")
    void sendEmail_DemoFallback_NotConfigured() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        boolean result = emailService.sendEmail("user@gmail.com", "Test Subject", "Test Body");

        assertTrue(result);
        verify(javaMailSender, never()).send(any(MimeMessage.class));
        // Check if file was written to tempDir
        assertTrue(tempDir.toFile().list().length > 0);
    }

    @Test
    @DisplayName("SendEmail - should fallback to demo outbox on SMTP failure")
    void sendEmail_DemoFallback_OnFailure() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Down"));

        boolean result = emailService.sendEmail("user@gmail.com", "Test Subject", "Test Body");

        assertTrue(result);
        assertTrue(tempDir.toFile().list().length > 0);
    }

    @Test
    @DisplayName("SendEmail - should return false when both SMTP and demo are disabled")
    void sendEmail_Disabled() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");
        ReflectionTestUtils.setField(emailService, "demoEmailEnabled", false);

        boolean result = emailService.sendEmail("user@gmail.com", "Test Subject", "Test Body");

        assertFalse(result);
    }
}
