package com.connectsphere.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
 * GatewayConfig
 * -------------
 * Configures CORS for the API Gateway.
 *
 * Why here?
 *   The Angular frontend runs on localhost:4200.
 *   Without CORS, the browser blocks cross-origin API calls.
 *   We configure CORS at the gateway level so individual
 *   services don't need to worry about it.
 *
 * Note: Uses reactive CorsWebFilter (not MVC CorsFilter)
 *       because Spring Cloud Gateway is WebFlux-based.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow Angular frontend
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // Allow common HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers (including Authorization for JWT)
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header so frontend can read JWT from responses
        config.setExposedHeaders(List.of("Authorization", "X-User-Email"));

        // Allow cookies/auth headers
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
    
}
