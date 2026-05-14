package com.connectsphere.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Starts Spring Boot Admin Server for monitoring registered services.
// UI is available at http://localhost:9090.
@SpringBootApplication
@EnableAdminServer
public class AdminServerApplication {

    public static void main(String[] args) {
        // Bootstrap Spring Boot Admin dashboard application context.
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
