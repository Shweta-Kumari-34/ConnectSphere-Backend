package com.connectsphere.like.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h1>ResourceNotFoundException</h1>
 * <p>Custom runtime exception indicating that a requested resource (e.g., a specific like or target entity) could not be found.</p>
 * 
 * <h2>Exception Handling Flow:</h2>
 * <pre>
 * graph TD
 *     A[Database Query] --> B{Entity Exists?}
 *     B -- No --> C[Throw ResourceNotFoundException]
 *     C --> D[Spring @ExceptionHandler]
 *     D --> E[Return HTTP 404 Not Found]
 * </pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
