package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data transfer object for requesting a One-Time Password (OTP).
 * <p>
 * Used in various flows such as initiating a password reset or email verification.
 * Requires a valid email address.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class OtpRequestDto {
 *         +String email
 *     }
 *     class AuthController {
 *         +generateOtp(OtpRequestDto)
 *     }
 *     class OtpService {
 *         +generateAndSendOtp(String email)
 *     }
 *     OtpRequestDto --> AuthController : Client Request
 *     AuthController --> OtpService : Forwards Email
 * </pre>
 */
@Data
public class OtpRequestDto {
    @NotBlank
    @Email
    private String email;
}
