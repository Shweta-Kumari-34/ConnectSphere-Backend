package com.connectsphere.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 * <p>
 * Exposes the API specifications for the Payment Service, detailing endpoints
 * for creating payment intents, confirming payments, and managing subscriptions.
 * </p>
 *
 * <h3>API Documentation Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[API Clients / Frontend] -->|Access Docs| B[Swagger UI];
 *     B --> C[Payment Controllers];
 * </pre>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Payment Service API")
                        .description("Payment Processing APIs — process payments, view transaction history")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
