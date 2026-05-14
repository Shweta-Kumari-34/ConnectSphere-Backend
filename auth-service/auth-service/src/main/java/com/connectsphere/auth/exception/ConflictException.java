package com.connectsphere.auth.exception;

/**
 * Exception thrown when a requested action conflicts with the current state of the system.
 * <p>
 * Commonly used for duplicate resource creation attempts, such as registering an
 * email or username that already exists. Results in a 409 Conflict HTTP status.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Logic] -->|Duplicate Found| B(throw ConflictException);
 *     B --> C[GlobalExceptionHandler];
 *     C --> D[409 Conflict];
 * </pre>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
