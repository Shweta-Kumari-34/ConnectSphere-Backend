package com.connectsphere.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.auth.entity.OtpEntity;

/**
 * OtpRepository — Database access layer for OTP (One-Time Password) records.
 *
 * Spring Data JPA automatically implements all these methods at runtime — we just define
 * the method signatures and Spring generates the SQL queries from the method names.
 *
 * Workflow for OTPs:
 *   1. When a user requests a login or signup OTP, a new OtpEntity row is created in the database.
 *   2. When the user submits the OTP, we look it up here to verify it.
 *   3. Once verified, the OTP is marked as "used = true" so it cannot be reused.
 *   4. Expired or already-used OTPs are rejected.
 */
@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    /**
     * Finds the active (unused) OTP for a given email and purpose (SIGNUP, LOGIN, RESET).
     *
     * This is the primary lookup used when the user submits their OTP code.
     * We only look for OTPs where used = false — this prevents a previously used code from
     * being accepted again (replay attack protection).
     *
     * Example: findByEmailAndPurposeAndUsedFalse("alice@email.com", "LOGIN")
     */
    Optional<OtpEntity> findByEmailAndPurposeAndUsedFalse(String email, String purpose);

    /**
     * Finds the most recently created OTP for an email, regardless of purpose or status.
     *
     * Used when we need to check when the last OTP was sent — for example, to enforce
     * a "wait 60 seconds before requesting another OTP" cooldown.
     */
    Optional<OtpEntity> findFirstByEmailOrderByExpiresAtDesc(String email);

    /**
     * Finds the most recently generated OTP for a specific email + purpose combination.
     *
     * Used to retrieve the latest OTP record when verifying — avoids confusion
     * if multiple OTPs were accidentally generated for the same user.
     */
    Optional<OtpEntity> findFirstByEmailAndPurposeOrderByIdDesc(String email, String purpose);

    /**
     * Counts how many active (non-expired) OTPs exist for an email + purpose.
     *
     * Used for rate limiting — if a user already has several active OTPs,
     * we can block them from generating more to prevent abuse.
     *
     * The expiresAt parameter filters out OTPs that have already expired.
     * Passing LocalDateTime.now() means "count only OTPs that haven't expired yet".
     */
    long countByEmailAndPurposeAndExpiresAtAfter(String email, String purpose, java.time.LocalDateTime expiresAt);
}
