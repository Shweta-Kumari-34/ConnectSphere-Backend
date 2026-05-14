package com.connectsphere.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Notification Service API")
                        .description("Notification delivery and read-state APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
