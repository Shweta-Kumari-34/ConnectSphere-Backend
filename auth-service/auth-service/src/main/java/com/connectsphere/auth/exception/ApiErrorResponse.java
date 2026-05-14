package com.connectsphere.auth.exception;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

/**
 * Unified data transfer object for all API error responses.
 * <p>
 * This class ensures a consistent error payload structure across all endpoints,
 * making it easier for frontend clients to parse and display meaningful error messages.
 * </p>
 *
 * <h3>Error Flow</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Controller / Service] -->|Throws Exception| B[GlobalExceptionHandler];
 *     B --> C[ApiErrorResponse Builder];
 *     C --> D[Standardized JSON Response to Client];
 * </pre>
 */
@Value
@Builder
public class ApiErrorResponse {
    Instant timestamp;
    int status;
    String error;
    String message;
    String path;
    Map<String, String> validationErrors;
}
