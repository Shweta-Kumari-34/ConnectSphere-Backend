package com.connectsphere.comment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts an action without proper permissions.
 * <p>
 * In the Comment Service, this is typically thrown when a user tries to edit
 * or delete a comment that they did not author. Results in an HTTP 401 response.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Verification] -->|Author Mismatch| B(throw UnauthorizedException);
 *     B --> C[Spring Error Handler];
 *     C --> D[401 Unauthorized Response];
 * </pre>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
