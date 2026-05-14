package com.connectsphere.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.connectsphere.auth.entity.OtpEntity;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.repository.OtpRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    private String email = "test@example.com";
    private String purpose = "SIGNUP";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("generateAndSendOtp - should save and send OTP")
    void generateAndSendOtp_Success() {
        when(otpRepository.findByEmailAndPurposeAndUsedFalse(anyString(), anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(emailService.sendOtpEmail(anyString(), anyString(), anyString())).thenReturn(true);

        otpService.generateAndSendOtp(email, purpose);

        verify(otpRepository).save(any(OtpEntity.class));
        verify(emailService).sendOtpEmail(eq(email), anyString(), eq(purpose));
    }

    @Test
    @DisplayName("generateAndSendOtp - should enforce resend cooldown")
    void generateAndSendOtp_Cooldown() {
        OtpEntity latest = new OtpEntity();
        latest.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // Issued just now
        when(otpRepository.findFirstByEmailAndPurposeOrderByIdDesc(anyString(), anyString())).thenReturn(Optional.of(latest));

        assertThrows(BadRequestException.class, () -> otpService.generateAndSendOtp(email, purpose));
    }

    @Test
    @DisplayName("verifyOtp - should return true for valid OTP")
    void verifyOtp_Success() {
        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setOtpCode("hashed_otp");
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        
        when(otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose)).thenReturn(Optional.of(otpEntity));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);

        boolean result = otpService.verifyOtp(email, "123456", purpose);

        assertTrue(result);
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
    }

    @Test
    @DisplayName("verifyOtp - should throw for expired OTP")
    void verifyOtp_Expired() {
        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        
        when(otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose)).thenReturn(Optional.of(otpEntity));

        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(email, "123456", purpose));
        assertTrue(otpEntity.isUsed());
    }

    @Test
    @DisplayName("verifyOtp - should throw and increment retry for invalid code")
    void verifyOtp_InvalidCode() {
        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setOtpCode("hashed_otp");
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpEntity.setRetryCount(0);
        
        when(otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose)).thenReturn(Optional.of(otpEntity));
        when(passwordEncoder.matches("wrong", "hashed_otp")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(email, "wrong", purpose));
        assertEquals(1, otpEntity.getRetryCount());
    }
}
