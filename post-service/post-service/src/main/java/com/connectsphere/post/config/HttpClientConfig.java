package com.connectsphere.post.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for initializing Spring's modern {@code RestClient}.
 * <p>
 * Used by the Post Service to communicate synchronously with the Search
 * and Media microservices.
 * </p>
 *
 * <h3>HTTP Client Context</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[HttpClientConfig] --> B[RestClient.Builder];
 *     B --> C[SearchServiceClient];
 *     B --> D[MediaServiceClient];
 * </pre>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
