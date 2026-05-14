package com.connectsphere.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard data transfer object for authentication responses.
 * <p>
 * This payload is returned to the client upon successful authentication operations,
 * such as login, registration, or token refresh. It contains the JWT token, user details,
 * and session status.
 * </p>
 *
 * <h3>Response Flow Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class AuthController {
 *         +login() AuthResponseDto
 *         +register() AuthResponseDto
 *     }
 *     class AuthResponseDto {
 *         +String message
 *         +String token
 *         +Long userId
 *         +String username
 *         +String role
 *         +boolean sessionEstablished
 *     }
 *     AuthController --> AuthResponseDto : Returns
 * </pre>
 */
@Data
@NoArgsConstructor
public class AuthResponseDto {
    private String message;
    private String token;
    private Long userId;
    private String username;
    private String role;
    private boolean sessionEstablished = true;

    public AuthResponseDto(String message, String token, Long userId, String username, String role) {
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.sessionEstablished = true;
    }

    public AuthResponseDto(String message, boolean sessionEstablished) {
        this.message = message;
        this.sessionEstablished = sessionEstablished;
    }
}
