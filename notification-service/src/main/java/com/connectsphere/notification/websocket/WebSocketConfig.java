package com.connectsphere.notification.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for real-time WebSocket communication using STOMP over SockJS.
 * <p>
 * Enables the in-app notification dropdown to receive real-time updates when 
 * an event (like/comment/follow) occurs, without requiring page reloads.
 * </p>
 *
 * <h3>WebSocket Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Angular Client] -->|SockJS Connect| B(/ws-notifications);
 *     B --> C[STOMP Broker];
 *     C -->|/user/queue/notifications| A;
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications").setAllowedOriginPatterns("*").withSockJS();
    }
}
