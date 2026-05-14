package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data transfer object for resetting a forgotten password.
 * <p>
 * Combines the verification step (email + OTP) with the action step (new password)
 * in a single secure request. Ensures the new password meets security requirements.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ResetPasswordRequestDto {
 *         +String email
 *         +String otp
 *         +String newPassword
 *     }
 *     class AuthController {
 *         +resetPassword(ResetPasswordRequestDto)
 *     }
 *     class AuthService {
 *         +resetPassword(String email, String otp, String newPassword)
 *     }
 *     ResetPasswordRequestDto --> AuthController : Recovery Payload
 *     AuthController --> AuthService : Executes Reset
 * </pre>
 */
@Data
public class ResetPasswordRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    private String newPassword;
}
