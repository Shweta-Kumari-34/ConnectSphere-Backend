package com.connectsphere.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    // This secret key is loaded from application.yml — it must be the same key
    // used by the API Gateway to verify tokens. Both sides share this one secret.
    @Value("${jwt.secret}")
    private String secretKey;

    // Token is valid for 24 hours. After that, the user needs to log in again.
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * Creates a JWT token for a user who just successfully logged in or registered.
     *
     * Here is what happens step by step:
     *   1. We convert the plain text secret key into a secure cryptographic key.
     *   2. We build the token — it contains the user's email (as the subject) and their role.
     *   3. We set the current time as the issue time, and add 24 hours as the expiry time.
     *   4. We sign the token with the secret key so it cannot be tampered with.
     *   5. We return the compact string — this is what the frontend stores and sends in every request.
     *
     * Next step after this: The token is returned in the login/register response.
     * The frontend stores it in localStorage and attaches it to every future API request
     * as an "Authorization: Bearer <token>" header.
     */
    public String generateToken(String email, String role) {
        // Step 1: Turn the plain secret into a real HMAC-SHA key
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        // Step 2-5: Build and sign the token, then return it as a compact string
        return Jwts.builder()
                .subject(email)                                                        // who this token belongs to
                .claim("role", role)                                                   // USER, ADMIN, or MODERATOR
                .issuedAt(new Date())                                                  // when the token was created
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))   // when it expires (24h from now)
                .signWith(key)                                                         // sign with secret so nobody can fake it
                .compact();                                                            // turn it into a readable string
    }
}