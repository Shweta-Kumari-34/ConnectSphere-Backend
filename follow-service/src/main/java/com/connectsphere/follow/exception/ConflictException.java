package com.connectsphere.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested action conflicts with the current state of the system.
 * <p>
 * In the Follow Service, this is typically thrown for business-rule conflicts,
 * such as a user attempting to follow someone they already follow, or attempting
 * to follow themselves. Results in an HTTP 409 Conflict response.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Logic] -->|Rule Violation| B(throw ConflictException);
 *     B --> C[Spring Error Handler];
 *     C --> D[409 Conflict Response];
 * </pre>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
