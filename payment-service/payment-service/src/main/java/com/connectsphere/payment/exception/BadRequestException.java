package com.connectsphere.payment.exception;

/**
 * Exception thrown when payment validation fails.
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Validation] -->|Failure| B(throw BadRequestException);
 *     B --> C[400 Response];
 * </pre>
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
