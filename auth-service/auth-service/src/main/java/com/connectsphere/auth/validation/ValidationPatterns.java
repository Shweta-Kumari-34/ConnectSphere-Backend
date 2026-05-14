package com.connectsphere.auth.validation;

// Shared regex patterns used by DTO validations and service-level checks.
public final class ValidationPatterns {

    // Utility class; prevent instantiation.
    private ValidationPatterns() {
    }

    // 3-20 chars: letters, digits, dot, underscore.
    public static final String USERNAME = "^(?=.{3,20}$)[a-zA-Z0-9._]+$";
    // At least 8 chars with one uppercase, one digit, and one special character.
    public static final String STRONG_PASSWORD = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    // Optional HTTP/HTTPS URL used for profile links and similar text fields.
    public static final String HTTP_URL = "^(https?://.*)?$";
}
