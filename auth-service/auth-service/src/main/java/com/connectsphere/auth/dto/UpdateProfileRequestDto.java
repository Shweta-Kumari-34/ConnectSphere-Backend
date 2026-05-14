package com.connectsphere.auth.dto;

import static com.connectsphere.auth.validation.ValidationPatterns.HTTP_URL;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for updating user profile information.
 * <p>
 * Allows users to update their public-facing details such as full name,
 * biography, and profile picture URL, subject to validation constraints.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class UpdateProfileRequestDto {
 *         +String fullName
 *         +String bio
 *         +String profilePicUrl
 *     }
 *     class AuthController {
 *         +updateProfile(UpdateProfileRequestDto)
 *     }
 *     class User {
 *         <<Entity>>
 *     }
 *     UpdateProfileRequestDto --> AuthController : Update Payload
 *     AuthController --> User : Applies Changes
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

    @Size(max = 80, message = "Full name must be under 80 characters")
    private String fullName;

    @Size(max = 250, message = "Bio must be under 250 characters")
    private String bio;

    @Pattern(regexp = HTTP_URL, message = "Profile picture URL must start with http:// or https://")
    private String profilePicUrl;
}
