package com.connectsphere.gateway.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Utility class for validating and parsing JSON Web Tokens (JWT) at the Gateway level.
 * 
 * <p>This class is responsible for:
 * <ul>
 *     <li>Verifying the signature of incoming tokens using the shared secret key.</li>
 *     <li>Extracting user identity (email) and authorization roles.</li>
 *     <li>Ensuring tokens are not expired or tampered with before routing to microservices.</li>
 * </ul>
 * 
 * <p>Note: This implementation uses the jjwt 0.12.6+ API, specifically utilizing
 * {@code verifyWith()} and {@code parseSignedClaims()} which replace the deprecated 
 * parserBuilder patterns.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Validate token and return claims
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract email (subject) from token
    public String extractEmail(String token) {
        return validateToken(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return validateToken(token).get("role", String.class);
    }
}
