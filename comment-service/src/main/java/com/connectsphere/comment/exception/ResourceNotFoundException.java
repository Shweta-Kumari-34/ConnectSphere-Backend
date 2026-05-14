package com.connectsphere.comment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested comment or parent comment does not exist.
 * <p>
 * Results in an HTTP 404 Not Found response. Usually triggered when attempting
 * to update, delete, or reply to a non-existent comment ID.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Repository Query] -->|Empty Optional| B(throw ResourceNotFoundException);
 *     B --> C[Spring Error Handler];
 *     C --> D[404 Response];
 * </pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
