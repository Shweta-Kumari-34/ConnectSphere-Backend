package com.connectsphere.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@connectsphere.com");
        ReflectionTestUtils.setField(emailService, "smtpPassword", "password123");
    }

    @Test
    @DisplayName("sendOtpEmail - should send email when SMTP is configured")
    void sendOtpEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = emailService.sendOtpEmail("user@example.com", "123456", "SIGNUP");

        assertTrue(result);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendOtpEmail - should handle different purposes")
    void sendOtpEmail_Purposes() {
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
        
        assertTrue(emailService.sendOtpEmail("user@example.com", "123456", "LOGIN"));
        assertTrue(emailService.sendOtpEmail("user@example.com", "123456", "RESET"));
        assertTrue(emailService.sendOtpEmail("user@example.com", "123456", "OTHER"));
        
        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendOtpEmail - should fail when SMTP is not configured")
    void sendOtpEmail_NoConfig() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");
        
        boolean result = emailService.sendOtpEmail("user@example.com", "123456", "SIGNUP");

        assertFalse(result);
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmail - should send async notification")
    void sendEmail_Success() {
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        emailService.sendEmail("user@example.com", "Test Subject", "Test Body");

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("validateMailConfiguration - should log status")
    void validateMailConfiguration_LogsStatus() {
        // PostConstruct is called manually here for coverage
        emailService.validateMailConfiguration();
        // Should complete without exception
    }
}
