package com.connectsphere.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.connectsphere.auth.dto.AuthResponseDto;
import com.connectsphere.auth.dto.ChangePasswordRequestDto;
import com.connectsphere.auth.dto.LoginRequestDto;
import com.connectsphere.auth.dto.RegisterRequestDto;
import com.connectsphere.auth.dto.UpdateProfileRequestDto;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.entity.VerificationRequest;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ConflictException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.repository.VerificationRequestRepository;
import com.connectsphere.auth.service.impl.AuthServiceImpl;
import com.connectsphere.auth.util.AuthConstants;
import com.connectsphere.auth.util.JwtUtil;

/**
 * AuthServiceImplTest — Unit tests for AuthServiceImpl using JUnit 5 + Mockito.
 *
 * PURPOSE:
 *   These tests verify that AuthServiceImpl behaves correctly under different conditions
 *   WITHOUT needing a running database, server, or email service.
 *
 * HOW IT WORKS (the "Mocking" concept):
 *   - Real dependencies (UserRepository, PasswordEncoder, JwtUtil) are replaced with
 *     "mock" (fake) versions using @Mock.
 *   - We tell the mocks exactly what to return using when(...).thenReturn(...).
 *   - This lets us test only the business logic inside AuthServiceImpl in isolation.
 *
 * WHAT IS COVERED HERE:
 *   1. register() — happy path + duplicate email + duplicate username
 *   2. login()    — happy path + user not found + wrong password
 *   3. changePassword() — happy path + wrong current password
 *
 * TESTING PATTERN USED (Arrange → Act → Assert):
 *   - Arrange: Set up the mocks and inputs.
 *   - Act:     Call the real service method.
 *   - Assert:  Check the output or that the correct exceptions were thrown.
 */
// @ExtendWith tells JUnit to use the Mockito framework to initialize @Mock and @InjectMocks fields.
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // ─── MOCK DEPENDENCIES ────────────────────────────────────────────────────
    // These are fake versions of the real dependencies.
    // They don't hit a real database — they just return whatever we tell them to.

    @Mock
    private UserRepository userRepository;  // fake database for users
    
    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder; // fake BCrypt encoder

    @Mock
    private JwtUtil jwtUtil;  // fake JWT token generator
    
    @Mock
    private OtpService otpService;
    
    @Mock
    private EmailService emailService;

    // ─── SYSTEM UNDER TEST ───────────────────────────────────────────────────
    // @InjectMocks creates a real AuthServiceImpl and automatically injects the @Mock
    // fields above into its constructor. This is the class we are actually testing.
    @InjectMocks
    private AuthServiceImpl authService;

    // ─── SHARED TEST DATA ─────────────────────────────────────────────────────
    // These are reused across multiple test methods to avoid repetition.
    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;
    private User testUser;

    /**
     * @BeforeEach runs before every single @Test method.
     * It resets the test data to a clean, known state before each test case.
     * This prevents one test from accidentally affecting another.
     */
    @BeforeEach
    void setUp() {
        // Build a sample registration request with valid data.
        registerRequest = new RegisterRequestDto();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setFullName("Test User");

        // Build a matching login request.
        loginRequest = new LoginRequestDto();
        loginRequest.setEmail("test@gmail.com");
        loginRequest.setPassword("Password@123");

        // Build a fake User object that represents the saved database record.
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@gmail.com");
        testUser.setPasswordHash("encodedPassword"); // simulates the BCrypt-hashed password stored in DB
        testUser.setRole("USER");
        testUser.setActive(true); // account is already verified and active
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTER TESTS
    // Testing: Does register() work correctly for all valid and invalid scenarios?
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Register - Success: should register new user and return token")
    void register_Success() {
        // Arrange: Set up the mocks to simulate a clean database (no duplicate email/username).
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);  // email is new → OK
        when(userRepository.existsByUsername("testuser")).thenReturn(false);     // username is new → OK
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword"); // simulate BCrypt hashing
        when(userRepository.save(any(User.class))).thenReturn(testUser);            // simulate saving and returning the user

        // Act: Call the real register method with our fake data.
        AuthResponseDto response = authService.register(registerRequest);
 
        // Assert: The response should contain the expected values.
        assertNotNull(response);
        assertEquals("OTP sent to email. Please verify to activate account.", response.getMessage());
        assertFalse(response.isSessionEstablished());
        verify(userRepository).save(any(User.class));                        // confirm save was actually called
        verify(otpService).generateAndSendOtp(eq("test@gmail.com"), eq(AuthConstants.OTP_PURPOSE_SIGNUP));
    }

    @Test
    @DisplayName("VerifyRegister - Success: should activate account and return token")
    void verifyRegister_Success() {
        // Arrange
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        testUser.setActive(false); // set to inactive for verification
        when(jwtUtil.generateToken("test@gmail.com", "USER")).thenReturn("jwt-token-123");

        // Act
        AuthResponseDto response = authService.verifyRegister("test@gmail.com", "123456");

        // Assert
        assertNotNull(response);
        assertEquals("Account verified and activated successfully", response.getMessage());
        assertEquals("jwt-token-123", response.getToken());
        assertTrue(testUser.isActive());
        verify(otpService).verifyOtp(eq("test@gmail.com"), eq("123456"), eq(AuthConstants.OTP_PURPOSE_SIGNUP));
        verify(userRepository).save(testUser);
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Register - Fail: should throw when email already exists")
    void register_DuplicateEmail() {
        // Arrange: Simulate database already having this email.
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true); // email is taken

        // Act & Assert: Expect a ConflictException with the right message.
        ConflictException ex = assertThrows(ConflictException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any()); // confirm that save was NEVER called — no partial saves
    }

    @Test
    @DisplayName("Register - Fail: should throw when username already exists")
    void register_DuplicateUsername() {
        // Arrange: Email is fine, but username is already taken.
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true); // username conflict

        // Act & Assert: Expect ConflictException for duplicate username.
        ConflictException ex = assertThrows(ConflictException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username already exists", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN TESTS
    // Testing: Does login() correctly authenticate users and reject bad credentials?
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Login - Success: should return token for valid credentials")
    void login_Success() {
        // Arrange: User exists in the DB and the password matches.
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(true); // password is correct
        when(jwtUtil.generateToken("test@gmail.com", "USER")).thenReturn("jwt-token-123");

        // Act: Call login with the correct credentials.
        AuthResponseDto response = authService.login(loginRequest);

        // Assert: Response has correct message and token.
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-token-123", response.getToken());
    }

    @Test
    @DisplayName("Login - Fail: should throw when user not found")
    void login_UserNotFound() {
        // Arrange: Simulate no user found for this email.
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty()); // no user in DB

        // Act & Assert: Expect ResourceNotFoundException when email doesn't exist.
        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login - Fail: should throw when password is invalid")
    void login_InvalidPassword() {
        // Arrange: User exists, but the submitted password does not match the stored hash.
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(false); // wrong password

        // Act & Assert: Expect BadRequestException with "Invalid password" message.
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid password", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHANGE PASSWORD TESTS
    // Testing: Does changePassword() correctly update the password and reject wrong inputs?
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ChangePassword - Success: should update password")
    void changePassword_Success() {
        // Arrange: Build the request with the correct current password and a new password.
        ChangePasswordRequestDto req = new ChangePasswordRequestDto();
        req.setCurrentPassword("Password@123");
        req.setNewPassword("NewPass@456");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(true); // current password is correct
        when(passwordEncoder.encode("NewPass@456")).thenReturn("newEncodedPassword");      // simulate hashing new password

        // Act: Call changePassword with the correct email and request.
        String result = authService.changePassword("test@gmail.com", req);

        // Assert: Success message returned and the user was saved with the new password hash.
        assertEquals("Password changed successfully", result);
        verify(userRepository).save(testUser); // confirm the updated user was written to the database
    }

    @Test
    @DisplayName("ChangePassword - Fail: should throw when current password wrong")
    void changePassword_WrongCurrentPassword() {
        // Arrange: User exists, but they typed the wrong current password.
        ChangePasswordRequestDto req = new ChangePasswordRequestDto();
        req.setCurrentPassword("wrongPassword");
        req.setNewPassword("NewPass@456");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false); // wrong!

        // Act & Assert: Expect BadRequestException — do NOT allow password change.
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.changePassword("test@gmail.com", req));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any()); // confirm no database write happened
    }

    @Test
    @DisplayName("InitiateLogin - Success: should send OTP")
    void initiateLogin_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        
        AuthResponseDto response = authService.initiateLogin("test@gmail.com");
        
        assertEquals("OTP sent to email for login", response.getMessage());
        verify(otpService).generateAndSendOtp(eq("test@gmail.com"), eq(AuthConstants.OTP_PURPOSE_LOGIN));
    }

    @Test
    @DisplayName("VerifyLogin - Success: should return token")
    void verifyLogin_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("test@gmail.com", "USER")).thenReturn("jwt-token-123");
        
        AuthResponseDto response = authService.verifyLogin("test@gmail.com", "123456");
        
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-token-123", response.getToken());
        verify(otpService).verifyOtp(eq("test@gmail.com"), eq("123456"), eq(AuthConstants.OTP_PURPOSE_LOGIN));
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UpdateProfile - Success: should update user fields")
    void updateProfile_Success() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto();
        request.setFullName("Updated Name");
        request.setBio("New Bio");
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        
        String result = authService.updateProfile("test@gmail.com", request);
        
        assertEquals("Profile updated successfully", result);
        assertEquals("Updated Name", testUser.getFullName());
        assertEquals("New Bio", testUser.getBio());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("DeactivateAccount - Success: should set user inactive")
    void deactivateAccount_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        
        String result = authService.deactivateAccount("test@gmail.com");
        
        assertEquals("Account deactivated successfully", result);
        assertFalse(testUser.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("SuspendUser - Success: should set user inactive by admin")
    void suspendUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        String result = authService.suspendUser(1L);
        
        assertTrue(result.contains("suspended"));
        assertFalse(testUser.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("ReactivateUser - Success: should set user active by admin")
    void reactivateUser_Success() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        String result = authService.reactivateUser(1L);
        
        assertTrue(result.contains("reactivated"));
        assertTrue(testUser.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("DeleteUserById - Success: should delete user from DB")
    void deleteUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        String result = authService.deleteUserById(1L);
        
        assertTrue(result.contains("deleted"));
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("ResetPassword - Success: should update password with OTP")
    void resetPassword_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("newEncodedHash");
        
        String result = authService.resetPassword("test@gmail.com", "123456", "NewPass@123");
        
        assertEquals("Password reset successfully", result);
        verify(otpService).verifyOtp(eq("test@gmail.com"), eq("123456"), eq(AuthConstants.OTP_PURPOSE_RESET));
        verify(userRepository).save(testUser);
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GetProfile - Success: should return profile map")
    void getProfile_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        
        Map<String, Object> profile = authService.getProfile("test@gmail.com");
        
        assertNotNull(profile);
        assertEquals("test@gmail.com", profile.get("email"));
        assertEquals("testuser", profile.get("username"));
    }

    @Test
    @DisplayName("SearchUsers - Success: should return list of users")
    void searchUsers_Success() {
        when(userRepository.findByUsernameContainingIgnoreCase("test")).thenReturn(List.of(testUser));
        when(userRepository.findByFullNameContainingIgnoreCase("test")).thenReturn(List.of());
        
        List<Map<String, Object>> results = authService.searchUsers("test");
        
        assertFalse(results.isEmpty());
        assertEquals("testuser", results.get(0).get("username"));
    }

    @Test
    @DisplayName("RefreshToken - Success: should return new token")
    void refreshToken_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("test@gmail.com", "USER")).thenReturn("new-token-456");
        
        AuthResponseDto response = authService.refreshToken("test@gmail.com");
        
        assertEquals("Token refreshed successfully", response.getMessage());
        assertEquals("new-token-456", response.getToken());
    }

    @Test
    @DisplayName("GetVerificationEligibility - Success: should check blockers")
    void getVerificationEligibility_Success() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        testUser.setProfilePicUrl(""); // Missing photo blocker
        
        Map<String, Object> eligibility = authService.getVerificationEligibility("test@gmail.com");
        
        assertFalse((Boolean) eligibility.get("eligible"));
        assertTrue(((List<?>) eligibility.get("blockers")).contains("Profile photo is required"));
    }

    @Test
    @DisplayName("ReviewVerificationRequest - Success: should approve request")
    void reviewVerificationRequest_Approve() {
        com.connectsphere.auth.entity.VerificationRequest vreq = new com.connectsphere.auth.entity.VerificationRequest();
        vreq.setId(1L);
        vreq.setStatus(com.connectsphere.auth.entity.VerificationRequest.VerificationStatus.SUBMITTED);
        
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(vreq));
        
        com.connectsphere.auth.dto.VerificationReviewRequestDto reviewReq = new com.connectsphere.auth.dto.VerificationReviewRequestDto();
        reviewReq.setDecision("APPROVE");
        reviewReq.setAdminNote("Looks good");
        
        Map<String, Object> result = authService.reviewVerificationRequest(1L, "admin@test.com", reviewReq);
        
        assertEquals("PAYMENT_PENDING", result.get("status"));
        verify(verificationRequestRepository).save(vreq);
    }

    @Test
    @DisplayName("ApplyInternalSubscriptionActivation - Success: should activate verified badge")
    void applyInternalSubscriptionActivation_Badge() {
        // Setup internal secret via reflection
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "internalSecretKey", "secret123");
        
        com.connectsphere.auth.dto.InternalSubscriptionActivationRequestDto req = new com.connectsphere.auth.dto.InternalSubscriptionActivationRequestDto();
        req.setUserEmail("test@gmail.com");
        req.setPlanCode("VERIFIED_BADGE");
        req.setActive(true);
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        
        Map<String, Object> result = authService.applyInternalSubscriptionActivation("secret123", req);
        
        assertTrue((Boolean) result.get("isVerified"));
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("InitiatePasswordReset - Success")
    void initiatePasswordReset_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        String result = authService.initiatePasswordReset("test@gmail.com");
        assertEquals("OTP sent to your email for password reset", result);
        verify(otpService).generateAndSendOtp(eq("test@gmail.com"), anyString());
    }

    @Test
    @DisplayName("GetAllUsers - Success")
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<Map<String, Object>> result = authService.getAllUsers();
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).get("username"));
    }

    @Test
    @DisplayName("ValidateVerificationUsername - Success/Failure")
    void validateVerificationUsername_Tests() {
        Map<String, Object> valid = authService.validateVerificationUsername("valid.user_123");
        assertTrue((Boolean) valid.get("valid"));
        Map<String, Object> invalid = authService.validateVerificationUsername("a");
        assertFalse((Boolean) invalid.get("valid"));
    }

    @Test
    @DisplayName("GetMyVerificationRequest - Success/Empty")
    void getMyVerificationRequest_Tests() {
        when(verificationRequestRepository.findFirstByUserEmailOrderBySubmittedAtDesc("test@gmail.com"))
                .thenReturn(Optional.empty());
        Map<String, Object> empty = authService.getMyVerificationRequest("test@gmail.com");
        assertEquals("NONE", empty.get("status"));

        VerificationRequest req = new VerificationRequest();
        req.setStatus(VerificationRequest.VerificationStatus.SUBMITTED);
        when(verificationRequestRepository.findFirstByUserEmailOrderBySubmittedAtDesc("test@gmail.com"))
                .thenReturn(Optional.of(req));
        Map<String, Object> found = authService.getMyVerificationRequest("test@gmail.com");
        assertEquals("SUBMITTED", found.get("status"));
    }

    @Test
    @DisplayName("GetVerificationRequestsForAdmin - Filters")
    void getVerificationRequestsForAdmin_Filters() {
        VerificationRequest req = new VerificationRequest();
        req.setStatus(VerificationRequest.VerificationStatus.SUBMITTED);
        when(verificationRequestRepository.findAll()).thenReturn(List.of(req));

        List<Map<String, Object>> all = authService.getVerificationRequestsForAdmin("ALL");
        assertEquals(1, all.size());

        List<Map<String, Object>> submitted = authService.getVerificationRequestsForAdmin("SUBMITTED");
        assertEquals(1, submitted.size());

        List<Map<String, Object>> rejected = authService.getVerificationRequestsForAdmin("REJECTED");
        assertEquals(0, rejected.size());
    }
}
