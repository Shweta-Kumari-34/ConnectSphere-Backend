package com.connectsphere.auth.dto;

import static com.connectsphere.auth.validation.ValidationPatterns.STRONG_PASSWORD;
import static com.connectsphere.auth.validation.ValidationPatterns.USERNAME;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for new account registrations.
 * <p>
 * Captures all necessary details to create a new user profile, enforcing strict
 * validation rules for usernames, email addresses, passwords, and full names.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class RegisterRequestDto {
 *         +String username
 *         +String email
 *         +String password
 *         +String fullName
 *         +String role
 *     }
 *     class AuthController {
 *         +register(RegisterRequestDto)
 *     }
 *     class User {
 *         <<Entity>>
 *     }
 *     RegisterRequestDto --> AuthController : Registration Form
 *     AuthController --> User : Maps to Entity
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = USERNAME, message = "Username must be 3-20 characters and use only letters, numbers, dots, or underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = STRONG_PASSWORD, message = "Password must be 8+ characters with one uppercase letter, one number, and one special character")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    private String fullName;

    @Pattern(regexp = "^(USER|ADMIN|MODERATOR)$", message = "Invalid role specified")
    private String role;
}
