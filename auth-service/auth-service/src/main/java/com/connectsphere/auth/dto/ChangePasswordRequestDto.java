package com.connectsphere.auth.dto;

import static com.connectsphere.auth.validation.ValidationPatterns.STRONG_PASSWORD;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for password change requests.
 * <p>
 * Used when an already authenticated user requests to change their password.
 * Validates that both the current and new passwords meet security constraints.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ChangePasswordRequestDto {
 *         +String currentPassword
 *         +String newPassword
 *     }
 *     class AuthController {
 *         +changePassword(ChangePasswordRequestDto)
 *     }
 *     ChangePasswordRequestDto --> AuthController : Validated Payload
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = STRONG_PASSWORD, message = "New password must be 8+ characters with one uppercase letter, one number, and one special character")
    private String newPassword;
}
