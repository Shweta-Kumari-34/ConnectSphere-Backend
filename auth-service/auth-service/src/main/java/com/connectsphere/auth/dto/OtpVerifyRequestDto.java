package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data transfer object for verifying a One-Time Password (OTP).
 * <p>
 * Used to confirm the user's possession of the email address before proceeding
 * with critical actions like password resets.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class OtpVerifyRequestDto {
 *         +String email
 *         +String otp
 *     }
 *     class AuthController {
 *         +verifyOtp(OtpVerifyRequestDto)
 *     }
 *     class OtpService {
 *         +verifyOtp(String email, String otp)
 *     }
 *     OtpVerifyRequestDto --> AuthController : Verification Payload
 *     AuthController --> OtpService : Validates OTP
 * </pre>
 */
@Data
public class OtpVerifyRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;
}
