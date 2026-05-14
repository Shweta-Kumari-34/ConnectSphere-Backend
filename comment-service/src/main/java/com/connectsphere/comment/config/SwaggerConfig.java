package com.connectsphere.comment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 * <p>
 * Exposes the API specifications for the Comment Service, detailing endpoints
 * for creating, updating, deleting, and fetching comments on posts or reels.
 * </p>
 *
 * <h3>API Documentation Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[API Clients / Frontend] -->|Access Docs| B[Swagger UI];
 *     B --> C[Comment Controllers];
 * </pre>
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines the base OpenAPI configuration for the Comment Service.
     * 
     * @return the configured {@link OpenAPI} object
     */
    @Bean
    public OpenAPI commentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Comment Service API")
                        .description("Comment management APIs for posts")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
