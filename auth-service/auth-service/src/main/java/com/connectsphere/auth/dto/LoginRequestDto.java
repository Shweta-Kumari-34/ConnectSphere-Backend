package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for standard password-based login requests.
 * <p>
 * Captures the user's email and plaintext password from the login form,
 * ensuring basic validation (non-blank, email format) before processing.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class LoginRequestDto {
 *         +String email
 *         +String password
 *     }
 *     class AuthController {
 *         +login(LoginRequestDto)
 *     }
 *     LoginRequestDto --> AuthController : Client Payload
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
