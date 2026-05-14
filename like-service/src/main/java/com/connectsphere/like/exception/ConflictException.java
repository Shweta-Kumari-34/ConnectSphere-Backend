package com.connectsphere.like.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h1>ConflictException</h1>
 * <p>Custom runtime exception indicating a state conflict, typically thrown when a user 
 * attempts to duplicate an engagement (e.g., liking a post twice).</p>
 * 
 * <h2>Exception Handling Flow:</h2>
 * <pre>
 * graph TD
 *     A[Business Logic Check] --> B{Already Liked?}
 *     B -- Yes --> C[Throw ConflictException]
 *     C --> D[Spring @ExceptionHandler]
 *     D --> E[Return HTTP 409 Conflict]
 * </pre>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
