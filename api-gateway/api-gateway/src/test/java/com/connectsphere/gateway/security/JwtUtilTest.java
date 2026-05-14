package com.connectsphere.gateway.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * JwtUtilTest — Unit tests for the API Gateway's JwtUtil.
 * Validates token parsing, email extraction, role extraction, and rejection of tampered tokens.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "connectsphere-super-secret-key-for-testing-only-1234";

    // A real JWT token signed with TEST_SECRET for "user@gmail.com" with role "USER"
    private String validToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);

        // Generate a real token to use in tests
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        validToken = Jwts.builder()
                .subject("user@gmail.com")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("extractEmail — should return correct email from valid token")
    void extractEmail_ValidToken_ReturnsEmail() {
        String email = jwtUtil.extractEmail(validToken);
        assertEquals("user@gmail.com", email);
    }

    @Test
    @DisplayName("extractRole — should return correct role from valid token")
    void extractRole_ValidToken_ReturnsRole() {
        String role = jwtUtil.extractRole(validToken);
        assertEquals("USER", role);
    }

    @Test
    @DisplayName("validateToken — should return non-null claims for valid token")
    void validateToken_ValidToken_ReturnsClaims() {
        var claims = jwtUtil.validateToken(validToken);
        assertNotNull(claims);
        assertEquals("user@gmail.com", claims.getSubject());
    }

    @Test
    @DisplayName("extractRole — ADMIN role should be extractable")
    void extractRole_AdminToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String adminToken = Jwts.builder()
                .subject("admin@gmail.com")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();

        assertEquals("ADMIN", jwtUtil.extractRole(adminToken));
        assertEquals("admin@gmail.com", jwtUtil.extractEmail(adminToken));
    }

    @Test
    @DisplayName("validateToken — should throw exception for tampered token")
    void validateToken_TamperedToken_ThrowsException() {
        String tampered = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        assertThrows(Exception.class, () -> jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("validateToken — should throw exception for expired token")
    void validateToken_ExpiredToken_ThrowsException() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("user@gmail.com")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000)) // already expired
                .signWith(key)
                .compact();

        assertThrows(Exception.class, () -> jwtUtil.validateToken(expiredToken));
    }
}
