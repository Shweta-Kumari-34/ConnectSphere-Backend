package com.connectsphere.auth.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * JwtUtilTest — Unit tests for JwtUtil token generation.
 * Tests the JWT token structure, email subject, and role claim.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // This must be at least 256 bits (32 chars) for HMAC-SHA256
    private static final String TEST_SECRET = "connectsphere-super-secret-key-for-testing-only-1234";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TEST_SECRET);
    }

    @Test
    @DisplayName("generateToken — should return non-null, non-empty token")
    void generateToken_ReturnsNonNullToken() {
        String token = jwtUtil.generateToken("test@gmail.com", "USER");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("generateToken — should return valid JWT format (3 parts)")
    void generateToken_ValidJwtFormat() {
        String token = jwtUtil.generateToken("test@gmail.com", "USER");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have exactly 3 parts: header.payload.signature");
    }

    @Test
    @DisplayName("generateToken — should produce unique tokens for different emails")
    void generateToken_UniquePerEmail() {
        String token1 = jwtUtil.generateToken("user1@gmail.com", "USER");
        String token2 = jwtUtil.generateToken("user2@gmail.com", "USER");
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("generateToken — should produce unique tokens for different roles")
    void generateToken_UniquePerRole() {
        String userToken = jwtUtil.generateToken("admin@gmail.com", "USER");
        String adminToken = jwtUtil.generateToken("admin@gmail.com", "ADMIN");
        assertNotEquals(userToken, adminToken);
    }

    @Test
    @DisplayName("generateToken — two calls with same args produce different tokens (due to issuedAt)")
    void generateToken_TimestampMakesUnique() throws InterruptedException {
        String token1 = jwtUtil.generateToken("user@gmail.com", "USER");
        Thread.sleep(1100); // Wait > 1s because JWT 'iat' is usually in seconds
        String token2 = jwtUtil.generateToken("user@gmail.com", "USER");
        // Tokens should differ because issuedAt changes
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("generateToken — ADMIN role should produce valid JWT")
    void generateToken_AdminRole() {
        String token = jwtUtil.generateToken("admin@gmail.com", "ADMIN");
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }
}
