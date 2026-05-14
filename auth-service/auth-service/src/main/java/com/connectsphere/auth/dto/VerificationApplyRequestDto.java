package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for users applying for a verification badge.
 * <p>
 * Captures the user's reasoning and flags indicating whether they have
 * included required identity documents or selfies for the review process.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class VerificationApplyRequestDto {
 *         +String reason
 *         +boolean includeDocument
 *         +boolean includeSelfie
 *     }
 *     class AuthController {
 *         +applyForVerification(VerificationApplyRequestDto)
 *     }
 *     class VerificationRequest {
 *         <<Entity>>
 *     }
 *     VerificationApplyRequestDto --> AuthController : Application Form
 *     AuthController --> VerificationRequest : Creates Request
 * </pre>
 */
@Data
@NoArgsConstructor
public class VerificationApplyRequestDto {

    @Size(max = 250, message = "Reason must be under 250 characters")
    private String reason;

    private boolean includeDocument;
    private boolean includeSelfie;
}
