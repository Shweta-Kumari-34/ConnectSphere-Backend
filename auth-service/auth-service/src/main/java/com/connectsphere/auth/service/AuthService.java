package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.AuthResponseDto;
import com.connectsphere.auth.dto.ChangePasswordRequestDto;
import com.connectsphere.auth.dto.InternalSubscriptionActivationRequestDto;
import com.connectsphere.auth.dto.LoginRequestDto;
import com.connectsphere.auth.dto.RegisterRequestDto;
import com.connectsphere.auth.dto.UpdateProfileRequestDto;
import com.connectsphere.auth.dto.VerificationApplyRequestDto;
import com.connectsphere.auth.dto.VerificationReviewRequestDto;
import com.connectsphere.auth.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * <h1>AuthService Interface</h1>
 * <p>The central contract for all identity and access management (IAM) operations within ConnectSphere.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>User Lifecycle:</b> Registration, activation via OTP, account deactivation, and permanent deletion.</li>
 *     <li><b>Authentication:</b> Multi-factor login (Password/OTP), JWT generation, and token refresh mechanisms.</li>
 *     <li><b>Credential Management:</b> Secure password reset and manual password changes.</li>
 *     <li><b>Profile Hub:</b> Comprehensive profile management including metadata updates and media uploads.</li>
 *     <li><b>Verification Workflow:</b> End-to-end processing of identity verification requests for blue badge eligibility.</li>
 *     <li><b>Admin Controls:</b> User suspension, reactivation, and verification request auditing.</li>
 * </ul>
 * 
 * <h2>System Flow Architecture:</h2>
 * <pre>
 * graph TD
 *     Start([User Action]) --> Auth{Authenticated?}
 *     Auth -- No --> Reg[Register/Login]
 *     Reg --> OTP[OTP Verification]
 *     OTP --> Session[JWT Issued]
 *     Auth -- Yes --> Ops[Profile/Verification/Admin Ops]
 *     Ops --> Persistence[(Database)]
 * </pre>
 */
public interface AuthService {

    /**
     * Initiates user registration. Saves inactive user and triggers signup OTP.
     */
    AuthResponseDto register(RegisterRequestDto request);

    /**
     * Validates signup OTP and activates the user account.
     */
    AuthResponseDto verifyRegister(String email, String otp);

    /**
     * Standard password-based login.
     */
    AuthResponseDto login(LoginRequestDto request);

    /**
     * Initiates login sequence by sending a login-specific OTP.
     */
    AuthResponseDto initiateLogin(String email);

    /**
     * Completes OTP-based login and issues JWT.
     */
    AuthResponseDto verifyLogin(String email, String otp);

    /**
     * Triggers password reset sequence via email OTP.
     */
    String initiatePasswordReset(String email);

    /**
     * Resets password using a valid reset OTP.
     */
    String resetPassword(String email, String otp, String newPassword);

    /**
     * Updates password for an authenticated user.
     */
    String changePassword(String email, ChangePasswordRequestDto request);

    /**
     * Updates basic profile information (Full Name, Bio, etc.).
     */
    String updateProfile(String email, UpdateProfileRequestDto request);

    /**
     * Handles profile picture upload and storage.
     */
    String uploadProfilePicture(String email, MultipartFile file);

    /**
     * Retrieves profile picture as a Resource.
     */
    Resource loadProfilePicture(String filename);

    /**
     * Retrieves verification documents as a Resource (Admin only).
     */
    Resource loadVerificationFile(String filename);

    /**
     * Fetches detailed profile information for a user.
     */
    Map<String, Object> getProfile(String email);

    /**
     * Fetches user profile by unique ID.
     */
    Map<String, Object> getUserById(Long userId);

    /**
     * Searches for users based on keyword (username/full name) with relevance boosting.
     */
    List<Map<String, Object>> searchUsers(String keyword);

    /**
     * Marks an account as inactive (soft delete).
     */
    String deactivateAccount(String email);

    /**
     * Generates a new JWT token for a valid session.
     */
    AuthResponseDto refreshToken(String email);

    /**
     * Lists all users in the system (Admin only).
     */
    List<Map<String, Object>> getAllUsers();

    /**
     * Suspends a user account (Admin only).
     */
    String suspendUser(Long userId);

    /**
     * Reactivates a suspended account (Admin only).
     */
    String reactivateUser(Long userId);

    /**
     * Permanently removes a user from the system (Admin only).
     */
    String deleteUserById(Long userId);

    /**
     * Validates if a username meets verification criteria.
     */
    Map<String, Object> validateVerificationUsername(String username);

    /**
     * Checks if a user meets prerequisites for verification request.
     */
    Map<String, Object> getVerificationEligibility(String email);

    /**
     * Submits a formal verification request with supporting documents.
     */
    Map<String, Object> submitVerificationRequest(String email, VerificationApplyRequestDto request, MultipartFile document, MultipartFile selfie);

    /**
     * Retrieves the status of the caller's verification request.
     */
    Map<String, Object> getMyVerificationRequest(String email);

    /**
     * Lists verification requests filtered by status (Admin only).
     */
    List<Map<String, Object>> getVerificationRequestsForAdmin(String statusFilter);

    /**
     * Processes an approval or rejection for a verification request (Admin only).
     */
    Map<String, Object> reviewVerificationRequest(Long requestId, String adminEmail, VerificationReviewRequestDto request);

    /**
     * Internal endpoint for programmatic subscription/badge activation.
     */
    Map<String, Object> applyInternalSubscriptionActivation(String internalSecret, InternalSubscriptionActivationRequestDto request);
}
