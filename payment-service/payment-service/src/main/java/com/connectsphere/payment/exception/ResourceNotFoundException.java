package com.connectsphere.payment.exception;

/**
 * Exception thrown when a payment or subscription cannot be found.
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[DB Query] -->|Not Found| B(throw ResourceNotFoundException);
 *     B --> C[404 Response];
 * </pre>
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
