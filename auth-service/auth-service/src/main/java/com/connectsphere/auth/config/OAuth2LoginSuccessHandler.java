package com.connectsphere.auth.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler invoked after a successful OAuth2 authentication (e.g., Google or GitHub).
 * <p>
 * This class is responsible for extracting the user's profile information from the OAuth2 provider,
 * checking if the user exists in the local database (auto-registering them if not),
 * generating a JWT token for standard platform authorization, and finally redirecting
 * the user back to the Angular frontend with the token and profile details.
 * </p>
 * 
 * <h3>Authentication Flow</h3>
 * <pre class="mermaid">
 * sequenceDiagram
 *     participant O as OAuth2 Provider
 *     participant H as SuccessHandler
 *     participant DB as UserRepository
 *     participant J as JwtUtil
 *     participant F as Frontend
 *     
 *     O->>H: Returns OAuth2User (Email, Name)
 *     H->>DB: Check if user exists by email
 *     alt User Not Found
 *         H->>DB: Auto-register new User
 *     end
 *     H->>J: Generate JWT token for User
 *     J-->>H: JWT Token
 *     H->>F: Redirect with token & user details
 * </pre>
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Constructs the OAuth2LoginSuccessHandler with necessary dependencies.
     * 
     * @param userRepository the repository for user data access
     * @param jwtUtil        the utility for generating JWT tokens
     */
    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Handles the successful authentication response from the OAuth2 provider.
     * 
     * @param request        the HTTP request
     * @param response       the HTTP response
     * @param authentication the authentication object containing the OAuth2 user details
     * @throws IOException if an input or output exception occurs during redirect
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract email — GitHub may return null for email attribute
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        // Determine provider
        String provider = "GOOGLE";
        if (email == null) {
            email = "oauth-user-" + System.currentTimeMillis() + "@connectsphere.com";
        }
        if (name == null) {
            name = email.split("@")[0];
        }

        // Find or create user in database
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Auto-register OAuth user
            user = new User();
            user.setEmail(email);
            user.setUsername(email.split("@")[0]);
            user.setFullName(name);
            user.setPasswordHash("OAUTH-" + provider); // No password for OAuth users
            user.setRole("USER");
            user.setProvider(provider);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setPremiumTheme("CLASSIC");
            user.setPremiumAutoRenew(true);

            // Handle potential username conflict
            if (userRepository.existsByUsername(user.getUsername())) {
                user.setUsername(user.getUsername() + "-" + System.currentTimeMillis() % 10000);
            }

            user = userRepository.save(user);
        } else {
            // Update provider if needed
            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider(provider);
                userRepository.save(user);
            }
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(email, user.getRole());

        // Redirect to frontend with token and user info
        String redirectUrl = frontendUrl + "/login?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                + "&role=" + URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8)
                + "&userId=" + user.getUserId()
                + "&provider=" + provider;

        response.sendRedirect(redirectUrl);
    }
}
