package com.connectsphere.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.auth.dto.AuthResponseDto;
import com.connectsphere.auth.dto.ChangePasswordRequestDto;
import com.connectsphere.auth.dto.InternalSubscriptionActivationRequestDto;
import com.connectsphere.auth.dto.LoginRequestDto;
import com.connectsphere.auth.dto.RegisterRequestDto;
import com.connectsphere.auth.dto.UpdateProfileRequestDto;
import com.connectsphere.auth.dto.VerificationApplyRequestDto;
import com.connectsphere.auth.dto.VerificationReviewRequestDto;
import com.connectsphere.auth.util.AuthConstants;
import com.connectsphere.auth.service.AuthService;

import jakarta.validation.Valid;

/**
 * AuthController — The front door of the Auth Service.
 *
 * Every request from the frontend related to users goes through here first.
 * This controller just receives the request, does light validation, and hands it
 * off to AuthService where the real logic lives.
 *
 * Flow overview:
 *   Frontend → API Gateway (JWT check) → Auth Service → AuthController → AuthService → Database
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    // These are loaded only to check if Google OAuth is properly configured at runtime.
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRATION FLOW
    // Step 1: User fills the form and submits → account is created (inactive)
    // Step 2: An OTP is sent to their email → user must verify to activate
    // ─────────────────────────────────────────────────────────────────────────

    // Step 1: Accepts user details, saves an inactive account, triggers OTP email.
    // Next: User checks their email and hits the verify endpoint below.
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Step 2: User enters the OTP from their email. If correct, account is activated
    // and a JWT token is returned so the user is immediately logged in.
    @PostMapping("/register/verify")
    public ResponseEntity<AuthResponseDto> verifyRegister(@Valid @RequestBody com.connectsphere.auth.dto.OtpVerifyRequestDto request) {
        return ResponseEntity.ok(authService.verifyRegister(request.getEmail(), request.getOtp()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN FLOW (Two Options)
    //
    // Option A: Password Login → email + password → immediate JWT if correct
    // Option B: OTP Login     → email only → OTP sent → verify → JWT
    // ─────────────────────────────────────────────────────────────────────────

    // Option A: Standard password-based login. Checks email + password, returns token.
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Option B Step 1: User provides only email → we send a one-time PIN to that email.
    // Next: User calls /login/verify with the OTP they received.
    @PostMapping("/login/initiate")
    public ResponseEntity<AuthResponseDto> initiateLogin(@Valid @RequestBody com.connectsphere.auth.dto.OtpRequestDto request) {
        return ResponseEntity.ok(authService.initiateLogin(request.getEmail()));
    }

    // Option B Step 2: User submits the OTP → we verify it and return a JWT token.
    @PostMapping("/login/verify")
    public ResponseEntity<AuthResponseDto> verifyLogin(@Valid @RequestBody com.connectsphere.auth.dto.OtpVerifyRequestDto request) {
        return ResponseEntity.ok(authService.verifyLogin(request.getEmail(), request.getOtp()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASSWORD RESET FLOW
    // Step 1: User submits email → OTP is sent to their inbox
    // Step 2: User submits email + OTP + new password → password is updated
    // ─────────────────────────────────────────────────────────────────────────

    // Step 1: Trigger OTP email for password reset.
    @PostMapping("/password-reset/initiate")
    public ResponseEntity<String> initiatePasswordReset(@Valid @RequestBody com.connectsphere.auth.dto.OtpRequestDto request) {
        return ResponseEntity.ok(authService.initiatePasswordReset(request.getEmail()));
    }

    // Step 2: Verify OTP and update the password. A confirmation email is also sent.
    @PostMapping("/password-reset/verify")
    public ResponseEntity<String> verifyPasswordReset(@Valid @RequestBody com.connectsphere.auth.dto.ResetPasswordRequestDto request) {
        return ResponseEntity.ok(authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword()));
    }

    // Logout is handled on the frontend by deleting the token from localStorage.
    // We don't maintain a token blacklist, so this endpoint just confirms the action.
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logged out successfully. Please remove token on client side.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OAUTH2 (Social Login) — Google / GitHub
    // Redirects the user to the provider's login page.
    // After login, the provider redirects back here with a code.
    // Spring Security handles the code exchange and creates/finds the user.
    // ─────────────────────────────────────────────────────────────────────────

    // Starts the Google OAuth2 flow. If credentials are not set up, returns a 503.
    @GetMapping("/oauth/google/start")
    public ResponseEntity<?> startGoogleOAuth() {
        if (!isGoogleOAuthConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "message", "Google OAuth is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET."
            ));
        }
        // Redirect the browser to Google's login page
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, AuthConstants.OAUTH_GOOGLE_AUTH_URL)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROFILE MANAGEMENT
    // The X-User-Email header is injected by the API Gateway after verifying
    // the JWT — so we always know exactly which user is making the request.
    // ─────────────────────────────────────────────────────────────────────────

    // Issues a fresh 24-hour token for a user who is already logged in.
    // Useful when the current token is about to expire.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.refreshToken(email));
    }

    // Returns all profile data for the currently logged-in user.
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.getProfile(email));
    }

    // Updates text fields (full name, bio, etc.) for the current user.
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    // Legacy alias for the same endpoint — kept for backward compatibility with old frontend code.
    @PutMapping("/update-profile")
    public ResponseEntity<String> updateProfileLegacy(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    // Accepts an image file (JPEG/PNG/WebP, max 5MB), saves it to the server,
    // and stores the public URL in the user's profile record.
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @RequestHeader("X-User-Email") String email,
            @RequestParam("file") MultipartFile file) {
        String profilePicUrl = authService.uploadProfilePicture(email, file);
        return ResponseEntity.ok(Map.of(
                "message", "Profile picture updated successfully",
                "profilePicUrl", profilePicUrl
        ));
    }

    // Serves a profile picture image by filename — called directly by <img> tags in the frontend.
    @GetMapping("/profile-picture/{filename:.+}")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable String filename) {
        Resource resource = authService.loadProfilePicture(filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.IMAGE_JPEG);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // Serves a verification document (ID photo, selfie) uploaded during badge verification.
    @GetMapping("/verification-file/{filename:.+}")
    public ResponseEntity<Resource> getVerificationFile(@PathVariable String filename) {
        Resource resource = authService.loadVerificationFile(filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // Changes the password for a logged-in user who knows their current password.
    // (Different from password reset, which is for users who forgot their password.)
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        return ResponseEntity.ok(authService.changePassword(email, request));
    }

    // Legacy alias — same as /password.
    @PutMapping("/change-password")
    public ResponseEntity<String> changePasswordLegacy(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        return ResponseEntity.ok(authService.changePassword(email, request));
    }

    // Searches users by username or full name. Used by the search bar in the frontend.
    // Results are sorted: premium users first, then verified, then alphabetical.
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(authService.searchUsers(q));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFICATION BADGE FLOW
    // Users can apply for a verified blue badge. The flow is:
    //   1. Check eligibility (account age, profile pic, username)
    //   2. Submit request with optional document + selfie
    //   3. Admin reviews the request and approves or rejects
    //   4. If approved, payment is triggered → badge is activated after payment
    // ─────────────────────────────────────────────────────────────────────────

    // Step 1: Check if the current user meets all requirements before letting them apply.
    @GetMapping("/verification/eligibility")
    public ResponseEntity<Map<String, Object>> getVerificationEligibility(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.getVerificationEligibility(email));
    }

    // Helper: Check if a specific username format qualifies for verification.
    @GetMapping("/verification/validate-username")
    public ResponseEntity<Map<String, Object>> validateVerificationUsername(@RequestParam String username) {
        return ResponseEntity.ok(authService.validateVerificationUsername(username));
    }

    // Step 2: User submits their verification application with optional supporting documents.
    @PostMapping(value = "/verification/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submitVerificationRequest(
            @RequestHeader("X-User-Email") String email,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "includeDocument", defaultValue = "false") boolean includeDocument,
            @RequestParam(value = "includeSelfie", defaultValue = "false") boolean includeSelfie,
            @RequestParam(value = "document", required = false) MultipartFile document,
            @RequestParam(value = "selfie", required = false) MultipartFile selfie) {

        VerificationApplyRequestDto request = new VerificationApplyRequestDto();
        request.setReason(reason);
        request.setIncludeDocument(includeDocument);
        request.setIncludeSelfie(includeSelfie);
        return ResponseEntity.ok(authService.submitVerificationRequest(email, request, document, selfie));
    }

    // Allows the user to check the current status of their verification request (SUBMITTED, APPROVED, etc.)
    @GetMapping("/verification/my-request")
    public ResponseEntity<Map<String, Object>> getMyVerificationRequest(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.getMyVerificationRequest(email));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN ENDPOINTS
    // These are protected by the Gateway — only ADMIN and MODERATOR roles can reach them.
    // ─────────────────────────────────────────────────────────────────────────

    // Admin sees all pending/approved/rejected verification requests, filterable by status.
    @GetMapping("/admin/verification-requests")
    public ResponseEntity<List<Map<String, Object>>> getVerificationRequestsForAdmin(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(authService.getVerificationRequestsForAdmin(status));
    }

    // Admin approves or rejects a specific verification request by ID.
    // If approved → status becomes PAYMENT_PENDING, waiting for payment confirmation.
    @PutMapping("/admin/verification-requests/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewVerificationRequest(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String adminEmail,
            @Valid @RequestBody VerificationReviewRequestDto request) {
        return ResponseEntity.ok(authService.reviewVerificationRequest(id, adminEmail, request));
    }

    // Called internally by the Payment Service (not the frontend) after a successful payment.
    // Activates either a Verified Badge or a Premium Membership on the user's account.
    // The X-Internal-Secret header ensures only trusted services can call this.
    @PostMapping("/internal/subscription-activation")
    public ResponseEntity<Map<String, Object>> applyInternalSubscriptionActivation(
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalSecret,
            @Valid @RequestBody InternalSubscriptionActivationRequestDto request) {
        return ResponseEntity.ok(authService.applyInternalSubscriptionActivation(internalSecret, request));
    }

    // User can deactivate their own account (soft delete — not permanently removed).
    @PutMapping("/deactivate")
    public ResponseEntity<String> deactivateAccount(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.deactivateAccount(email));
    }

    // Admin: Get all users in the system.
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // Public: Look up any user's profile by their email address (used by other services too).
    @GetMapping("/user/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getProfile(email));
    }

    // Public: Look up any user's profile by their numeric ID.
    @GetMapping("/user/id/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    // Admin: Ban a user (sets isActive = false). They can no longer log in.
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<String> suspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.suspendUser(id));
    }

    // Admin: Restore a previously suspended user's access.
    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<String> reactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.reactivateUser(id));
    }

    // Admin: Permanently delete a user account and all their data.
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.deleteUserById(id));
    }

    // Health check — used by Eureka and the Admin Server to confirm this service is alive.
    @GetMapping("/test")
    public String test() {
        return "Auth Service is running";
    }

    // Checks if real Google credentials are configured (not empty or dummy placeholders).
    // If not configured, the OAuth endpoint will gracefully return an error instead of crashing.
    private boolean isGoogleOAuthConfigured() {
        String clientId = googleClientId == null ? "" : googleClientId.trim();
        String clientSecret = googleClientSecret == null ? "" : googleClientSecret.trim();
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            return false;
        }
        return !(AuthConstants.DUMMY_CLIENT_ID.equalsIgnoreCase(clientId) || AuthConstants.DUMMY_CLIENT_SECRET.equals(clientSecret));
    }
}
