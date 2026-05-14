package com.connectsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for administrators reviewing verification applications.
 * <p>
 * Captures the admin's decision (APPROVE or REJECT) along with optional notes
 * and reasons for rejection, ensuring accountability in the review process.
 * </p>
 *
 * <h3>Data Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class VerificationReviewRequestDto {
 *         +String decision
 *         +String rejectionReason
 *         +String adminNote
 *     }
 *     class AdminController {
 *         +reviewVerification(VerificationReviewRequestDto)
 *     }
 *     class AuthService {
 *         +processVerificationDecision()
 *     }
 *     VerificationReviewRequestDto --> AdminController : Admin Decision
 *     AdminController --> AuthService : Updates Status
 * </pre>
 */
@Data
@NoArgsConstructor
public class VerificationReviewRequestDto {

    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Decision must be APPROVE or REJECT")
    private String decision;

    @Size(max = 300, message = "Rejection reason must be under 300 characters")
    private String rejectionReason;

    @Size(max = 300, message = "Admin note must be under 300 characters")
    private String adminNote;
}
