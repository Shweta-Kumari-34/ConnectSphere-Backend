package com.connectsphere.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.payment.entity.Payment;

/*
 * PaymentRepository
 * -----------------
 * Database operations for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find all payments by a specific user's email
    List<Payment> findByUserEmail(String userEmail);

    // Find payments by status
    List<Payment> findByStatus(Payment.PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
