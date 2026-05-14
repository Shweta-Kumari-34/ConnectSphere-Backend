package com.connectsphere.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to interact with media without permissions.
 * <p>
 * In the Media Service, this is typically thrown when a user tries to delete
 * a story or reel that they did not originally upload. Results in a 401 response.
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
