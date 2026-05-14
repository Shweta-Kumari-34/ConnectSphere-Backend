package com.connectsphere.auth.exception;

/**
 * Exception thrown when a client request contains invalid or malformed data.
 * <p>
 * This typically results in a 400 Bad Request HTTP status code.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Service Validation] -->|Invalid Data| B(throw BadRequestException);
 *     B --> C[GlobalExceptionHandler];
 *     C --> D[400 Bad Request];
 * </pre>
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
