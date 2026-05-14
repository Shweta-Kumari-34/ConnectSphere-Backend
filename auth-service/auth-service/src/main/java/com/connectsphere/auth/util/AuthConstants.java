package com.connectsphere.auth.util;

/**
 * Centralized constants for the Auth Service to avoid magic strings
 * and ensure consistency across services and controllers.
 */
public final class AuthConstants {

    private AuthConstants() {
        // Prevent instantiation
    }

    // Roles
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MODERATOR = "MODERATOR";

    // Providers
    public static final String PROVIDER_LOCAL = "LOCAL";
    public static final String PROVIDER_GOOGLE = "GOOGLE";
    public static final String PROVIDER_GITHUB = "GITHUB";

    // Themes
    public static final String THEME_CLASSIC = "CLASSIC";

    // OTP Purposes
    public static final String OTP_PURPOSE_SIGNUP = "SIGNUP";
    public static final String OTP_PURPOSE_LOGIN = "LOGIN";
    public static final String OTP_PURPOSE_RESET = "RESET";

    // Plan Codes
    public static final String PLAN_VERIFIED_BADGE = "VERIFIED_BADGE";
    public static final String PLAN_PREMIUM_MEMBERSHIP = "PREMIUM_MEMBERSHIP";

    // OAuth Constants
    public static final String OAUTH_GOOGLE_AUTH_URL = "/oauth2/authorization/google";
    public static final String DUMMY_CLIENT_ID = "dummy-id";
    public static final String DUMMY_CLIENT_SECRET = "dummy-secret";

    // File Directories
    public static final String DEFAULT_PROFILE_PICTURE_DIR = "./uploads/profile-pictures";
    public static final String DEFAULT_VERIFICATION_DIR = "./uploads/verification";

    // Error Messages
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_INVALID_OTP = "Invalid or expired OTP";
    public static final String ERROR_PASSWORD_LENGTH = "Password must be at least 8 characters long";
}
