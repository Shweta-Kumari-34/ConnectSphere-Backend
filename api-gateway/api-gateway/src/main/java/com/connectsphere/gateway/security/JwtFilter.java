package com.connectsphere.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Custom Gateway Filter for handling JSON Web Token (JWT) authentication.
 * 
 * <p>This filter acts as the primary security layer for the microservices cluster:
 * <ul>
 *     <li>Bypasses security for public endpoints (Registration, Login, etc.).</li>
 *     <li>Supports "Guest Browsing" mode where GET requests for feeds are allowed without a token.</li>
 *     <li>Validates the "Authorization: Bearer" header for protected routes.</li>
 *     <li>Extracts user identity (Email, Role) and injects them as custom headers (X-User-Email, X-User-Role).</li>
 *     <li>Enforces Role-Based Access Control (RBAC) for Admin/Moderator routes.</li>
 * </ul>
 */
@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            String path = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethod().name();

            // CORS preflight — must pass through without any JWT check
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return chain.filter(exchange);
            }

            // Public endpoints — no JWT required (Guest browsing per case study §2.2)
            if (path.contains("/auth/register") || 
                path.contains("/auth/login") || 
                path.contains("/auth/password-reset") ||
                path.contains("/auth/test") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.endsWith("/test")) {
                return chain.filter(exchange);
            }

            // Guest browsing: GET on public feeds, profiles, search, trending
            // Allows unauthenticated browsing per case study §2.2 Guest requirements
            if ("GET".equals(method) && (
                // Post browsing
                path.equals("/posts/feed") ||
                path.equals("/posts/all") ||
                path.startsWith("/posts/user/") ||
                path.matches("/posts/\\d+") ||
                path.equals("/posts/search") ||
                path.startsWith("/posts/count/") ||
                // Search & discovery
                path.equals("/search/trending") ||
                path.equals("/search/posts") ||
                path.equals("/search/posts-by-hashtag") ||
                path.equals("/search/hashtags") ||
                path.equals("/search/users") ||
                path.equals("/search/count") ||
                path.startsWith("/search/hashtags/post/") ||
                // User profiles (public)
                path.startsWith("/auth/user/") ||
                path.equals("/auth/search") ||
                path.startsWith("/auth/profile-picture/") ||
                // Media
                path.startsWith("/media/stories/active") ||
                path.startsWith("/media/post/") ||
                // Follow counts (public)
                path.startsWith("/follows/follower-count/") ||
                path.startsWith("/follows/following-count/") ||
                path.startsWith("/follows/followers/") ||
                path.startsWith("/follows/following/") ||
                // Reels (public feeds)
                path.startsWith("/reels/feed/") ||
                path.startsWith("/reels/my-reels/") ||
                path.startsWith("/reels/user/") ||
                path.startsWith("/uploads/") ||
                path.equals("/reels/explore"))) {
                
                // Allow through but still inject email if token present
                if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    try {
                        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring(7);
                        jwtUtil.validateToken(token);
                        String email = jwtUtil.extractEmail(token);
                        String role = jwtUtil.extractRole(token);
                        if (role == null) {
                            role = "USER";
                        }
                        exchange = exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                        .header("X-User-Email", email)
                                        .header("X-User-Role", role)
                                        .build())
                                .build();
                    } catch (Exception ignored) {}
                }
                return chain.filter(exchange);
            }

            // All other routes require JWT
            if (!exchange.getRequest().getHeaders().containsKey(org.springframework.http.HttpHeaders.AUTHORIZATION)) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            try {
                jwtUtil.validateToken(token);
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                if (role == null) role = "USER";

                // Protect Admin/Moderator routes
                if (path.contains("/auth/admin/") || path.contains("/reports") || path.matches("/auth/users(/.*)?")) {
                     if (!role.equals("ADMIN") && !role.equals("MODERATOR")) {
                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                     }
                }

                exchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-User-Email", email)
                                .header("X-User-Role", role)
                                .build())
                        .build();

            } catch (Exception e) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
