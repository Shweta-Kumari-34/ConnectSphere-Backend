package com.connectsphere.auth.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.connectsphere.auth.entity.OtpEntity;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.repository.OtpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h1>OtpService</h1>
 * <p>Handles the generation, distribution, and validation of One-Time Passwords (OTP) 
 * for critical security events such as registration, login, and password resets.</p>
 * 
 * <h2>OTP Lifecycle Flow:</h2>
 * <pre>
 * graph LR
 *     Gen[Generate 6-digit Code] --> Hash[Bcrypt Hash Code]
 *     Hash --> Save[Save to DB/Cache]
 *     Save --> Send[Send via EmailService]
 *     Send --> Verify{User Enters Code}
 *     Verify -- Matches --> Success[Mark Used & Grant Access]
 *     Verify -- Fails --> Retry[Increment Retry Count]
 *     Retry -- >5 Fails --> Lock[Invalidate OTP]
 * </pre>
 * 
 * <h2>Security Measures:</h2>
 * <ul>
 *     <li><b>Rate Limiting:</b> Enforces cooldown periods (60s) and max requests per window (5/10min).</li>
 *     <li><b>Expiry:</b> OTPs are strictly valid for 10 minutes.</li>
 *     <li><b>Hashing:</b> OTP codes are stored as salted hashes (BCrypt) to prevent DB-leak exposure.</li>
 *     <li><b>One-Time Use:</b> Codes are immediately invalidated upon successful verification.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS_PER_WINDOW = 5;
    private static final int RESEND_WINDOW_MINUTES = 10;

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public void generateAndSendOtp(String email, String purpose) {
        String normalizedEmail = email.trim().toLowerCase();
        enforceResendLimits(normalizedEmail, purpose);

        // Invalidate any existing unused OTP for this purpose
        otpRepository.findByEmailAndPurposeAndUsedFalse(normalizedEmail, purpose)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    otpRepository.save(existing);
                });

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));

        log.info("\n=======================================================");
        log.info("DEVELOPMENT MODE: OTP FOR {} IS -> {}", normalizedEmail, otpCode);
        log.info("=======================================================\n");

        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setEmail(normalizedEmail);
        otpEntity.setOtpCode(passwordEncoder.encode(otpCode));
        otpEntity.setPurpose(purpose);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpRepository.save(otpEntity);

        // In local/dev environments SMTP may be absent; keep OTP flow available via DB/logs.
        boolean sent = emailService.sendOtpEmail(normalizedEmail, otpCode, purpose);
        if (!sent) {
            log.warn("SMTP not configured; OTP for {} was generated but not emailed.", normalizedEmail);
        }
    }

    public boolean verifyOtp(String email, String otpCode, String purpose) {
        String normalizedEmail = email.trim().toLowerCase();
        OtpEntity otpEntity = otpRepository.findByEmailAndPurposeAndUsedFalse(normalizedEmail, purpose)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new BadRequestException("OTP has expired");
        }

        if (!passwordEncoder.matches(otpCode, otpEntity.getOtpCode())) {
            otpEntity.setRetryCount(otpEntity.getRetryCount() + 1);
            if (otpEntity.getRetryCount() >= 5) {
                otpEntity.setUsed(true); // lock it after 5 retries
            }
            otpRepository.save(otpEntity);
            throw new BadRequestException("Invalid OTP code");
        }

        // OTP is valid
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);
        return true;
    }

    public String getLatestOtpForDebug(String email) {
        return "OTP debug access is disabled for security";
    }

    private void enforceResendLimits(String email, String purpose) {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.findFirstByEmailAndPurposeOrderByIdDesc(email, purpose)
                .ifPresent(latest -> {
                    LocalDateTime issuedAt = latest.getExpiresAt().minusMinutes(OTP_EXPIRY_MINUTES);
                    if (issuedAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
                        throw new BadRequestException("Please wait 60 seconds before requesting a new OTP");
                    }
                });

        long recentCount = otpRepository.countByEmailAndPurposeAndExpiresAtAfter(
                email, purpose, now.minusMinutes(RESEND_WINDOW_MINUTES).plusMinutes(OTP_EXPIRY_MINUTES));
        if (recentCount >= MAX_RESENDS_PER_WINDOW) {
            throw new BadRequestException("Too many OTP requests. Please try again later");
        }
    }
}
