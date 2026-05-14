package com.connectsphere.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global security configuration for the Auth Service.
 * <p>
 * This class configures CORS policies, disables CSRF (since this is a stateless REST API),
 * defines route authorization rules (e.g., making /auth/** public), and integrates
 * the OAuth2 login flow with a custom success handler.
 * </p>
 *
 * <h3>Security Filter Chain Architecture</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Incoming HTTP Request] --> B[CORS Filter];
 *     B --> C[Security Filter Chain];
 *     C --> D{Is Route Public?};
 *     D -->|Yes (/auth/**, /oauth2/**)| E[Proceed to Controller];
 *     D -->|No| F{Is Authenticated?};
 *     F -->|Yes| E;
 *     F -->|No| G[401 Unauthorized];
 * </pre>
 */
@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /**
     * Constructs the SecurityConfig with required dependencies.
     * 
     * @param oAuth2LoginSuccessHandler the success handler for OAuth2 logins
     */
    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    /**
     * Provides a BCrypt password encoder bean for hashing passwords.
     * 
     * @return the configured {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the main security filter chain for HTTP requests.
     * <p>
     * - Configures CORS and disables CSRF.
     * - Permits unauthenticated access to /auth/**, /oauth2/**, and /login/oauth2/** endpoints.
     * - Requires authentication for all other requests.
     * - Configures OAuth2 login with the custom success handler.
     * </p>
     * 
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Stateless REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            );

        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * <p>
     * Specifically allows requests from the Angular frontend (http://localhost:4200).
     * </p>
     * 
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}