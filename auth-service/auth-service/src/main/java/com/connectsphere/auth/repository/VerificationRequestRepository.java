package com.connectsphere.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.auth.entity.VerificationRequest;
import com.connectsphere.auth.entity.VerificationRequest.VerificationStatus;

// Repository for verification badge request workflows.
@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    // Fetches the latest request of a user for profile and eligibility checks.
    Optional<VerificationRequest> findFirstByUserEmailOrderBySubmittedAtDesc(String userEmail);

    // Returns pending-like admin queue ordered oldest-first for fair review.
    List<VerificationRequest> findAllByStatusInOrderBySubmittedAtAsc(List<VerificationStatus> statuses);
}
