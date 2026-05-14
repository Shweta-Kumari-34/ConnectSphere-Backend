package com.connectsphere.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an uploaded file violates validation rules.
 * <p>
 * In the Media Service, this is typically thrown for unsupported file types
 * or files that exceed the maximum allowed size configured in {@code UploadConfig}.
 * </p>
 *
 * <h3>Usage Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[File Validator] -->|Invalid File| B(throw BadRequestException);
 *     B --> C[Spring Error Handler];
 *     C --> D[400 Bad Request];
 * </pre>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
