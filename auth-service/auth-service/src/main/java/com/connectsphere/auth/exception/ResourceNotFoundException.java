package com.connectsphere.auth.exception;

/**
 * Exception thrown when a requested entity or resource cannot be found in the database.
 * <p>
 * Results in a 404 Not Found HTTP status code. Used extensively when fetching users
 * by ID or email that do not exist.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Repository Query] -->|Returns Empty| B(throw ResourceNotFoundException);
 *     B --> C[GlobalExceptionHandler];
 *     C --> D[404 Not Found];
 * </pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
