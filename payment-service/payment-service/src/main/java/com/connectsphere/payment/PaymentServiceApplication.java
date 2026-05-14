package com.connectsphere.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * PaymentServiceApplication
 * -------------------------
 * Main class for the Payment Service microservice.
 *
 * Responsibilities:
 *   - Handle payment/subscription operations
 *   - Store payment records in payment_db (MySQL)
 *   - Register with Eureka for service discovery
 *
 * Port: 8083
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
