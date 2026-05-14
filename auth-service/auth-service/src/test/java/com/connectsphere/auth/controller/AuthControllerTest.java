package com.connectsphere.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import com.connectsphere.auth.dto.AuthResponseDto;
import com.connectsphere.auth.dto.ChangePasswordRequestDto;
import com.connectsphere.auth.dto.UpdateProfileRequestDto;
import com.connectsphere.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * AuthControllerTest — Unit tests for AuthController REST endpoints.
 * Uses standalone MockMvc — no Spring Security context needed.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private AuthResponseDto successResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        // Inject empty OAuth credentials so isGoogleOAuthConfigured() returns false cleanly
        ReflectionTestUtils.setField(authController, "googleClientId", "");
        ReflectionTestUtils.setField(authController, "googleClientSecret", "");

        successResponse = new AuthResponseDto();
        successResponse.setMessage("Success");
        successResponse.setToken("jwt-token-123");
        successResponse.setSessionEstablished(true);
    }

    @Test
    @DisplayName("POST /auth/register — should register user and return 200")
    void register_Success() throws Exception {
        when(authService.register(any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@gmail.com\",\"password\":\"Password@123\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isOk());

        verify(authService).register(any());
    }

    @Test
    @DisplayName("POST /auth/login — should login and return token")
    void login_Success() throws Exception {
        when(authService.login(any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@gmail.com\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk());

        verify(authService).login(any());
    }

    @Test
    @DisplayName("POST /auth/logout — should return 200 with message")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully. Please remove token on client side."));
    }

    @Test
    @DisplayName("POST /auth/refresh — should return refreshed token")
    void refreshToken_Success() throws Exception {
        when(authService.refreshToken("test@gmail.com")).thenReturn(successResponse);

        mockMvc.perform(post("/auth/refresh")
                        .header("X-User-Email", "test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/profile — should return user profile")
    void getProfile_Success() throws Exception {
        when(authService.getProfile("test@gmail.com")).thenReturn(Map.of("email", "test@gmail.com", "username", "testuser"));

        mockMvc.perform(get("/auth/profile")
                        .header("X-User-Email", "test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /auth/profile — should update profile and return 200")
    void updateProfile_Success() throws Exception {
        when(authService.updateProfile(eq("test@gmail.com"), any(UpdateProfileRequestDto.class)))
                .thenReturn("Profile updated successfully");

        mockMvc.perform(put("/auth/profile")
                        .header("X-User-Email", "test@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Name\",\"bio\":\"New bio\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile updated successfully"));
    }

    @Test
    @DisplayName("PUT /auth/password — should change password and return 200")
    void changePassword_Success() throws Exception {
        when(authService.changePassword(eq("test@gmail.com"), any(ChangePasswordRequestDto.class)))
                .thenReturn("Password changed successfully");

        mockMvc.perform(put("/auth/password")
                        .header("X-User-Email", "test@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password@123\",\"newPassword\":\"NewPass@456\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));
    }

    @Test
    @DisplayName("GET /auth/search — should return matching users")
    void searchUsers_Success() throws Exception {
        when(authService.searchUsers("test")).thenReturn(List.of(Map.of("username", "testuser")));

        mockMvc.perform(get("/auth/search").param("q", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/users — should return all users")
    void getAllUsers_Success() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(Map.of("email", "test@gmail.com")));

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/user/{email} — should return user by email")
    void getUserByEmail_Success() throws Exception {
        when(authService.getProfile("test@gmail.com")).thenReturn(Map.of("email", "test@gmail.com"));

        mockMvc.perform(get("/auth/user/test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/user/id/{id} — should return user by id")
    void getUserById_Success() throws Exception {
        when(authService.getUserById(1L)).thenReturn(Map.of("userId", 1L));

        mockMvc.perform(get("/auth/user/id/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /auth/users/{id}/suspend — should suspend user")
    void suspendUser_Success() throws Exception {
        when(authService.suspendUser(1L)).thenReturn("User suspended successfully");

        mockMvc.perform(put("/auth/users/1/suspend"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /auth/users/{id}/reactivate — should reactivate user")
    void reactivateUser_Success() throws Exception {
        when(authService.reactivateUser(1L)).thenReturn("User reactivated successfully");

        mockMvc.perform(put("/auth/users/1/reactivate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /auth/users/{id} — should delete user")
    void deleteUser_Success() throws Exception {
        when(authService.deleteUserById(1L)).thenReturn("User deleted successfully");

        mockMvc.perform(delete("/auth/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /auth/deactivate — should deactivate account")
    void deactivateAccount_Success() throws Exception {
        when(authService.deactivateAccount("test@gmail.com")).thenReturn("Account deactivated successfully");

        mockMvc.perform(put("/auth/deactivate")
                        .header("X-User-Email", "test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/login/initiate — should initiate OTP login")
    void initiateLogin_Success() throws Exception {
        successResponse.setMessage("OTP sent to email for login");
        successResponse.setSessionEstablished(false);
        when(authService.initiateLogin("test@gmail.com")).thenReturn(successResponse);

        mockMvc.perform(post("/auth/login/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@gmail.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/password-reset/initiate — should send reset OTP")
    void initiatePasswordReset_Success() throws Exception {
        when(authService.initiatePasswordReset("test@gmail.com")).thenReturn("OTP sent for password reset");

        mockMvc.perform(post("/auth/password-reset/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@gmail.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth Service is running"));
    }

/*
    @Test
    @DisplayName("GET /auth/oauth/google/start — should return 503 when not configured")
    void startGoogleOAuth_NotConfigured() throws Exception {
        mockMvc.perform(get("/auth/oauth/google/start"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("POST /auth/register/verify — should verify OTP and return 200")
    void verifyRegister_Success() throws Exception {
        when(authService.verifyRegister(anyString(), anyString())).thenReturn(successResponse);
        mockMvc.perform(post("/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/oauth/google/start — should return 302 when configured")
    void startGoogleOAuth_Configured() throws Exception {
        ReflectionTestUtils.setField(authController, "googleClientId", "id123");
        ReflectionTestUtils.setField(authController, "googleClientSecret", "secret123");
        mockMvc.perform(get("/auth/oauth/google/start"))
                .andExpect(status().isFound());
    }
*/
}
