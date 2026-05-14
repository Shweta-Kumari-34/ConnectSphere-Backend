package com.connectsphere.follow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Entry point for follow-service microservice.
@SpringBootApplication
public class FollowServiceApplication {
    // Boots Spring context and starts embedded server.
    public static void main(String[] args) { SpringApplication.run(FollowServiceApplication.class, args); }
}
