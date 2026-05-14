package com.connectsphere.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.payment.entity.Subscription;
import com.connectsphere.payment.entity.Subscription.SubscriptionStatus;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserEmailOrderByStartedAtDesc(String userEmail);

    Optional<Subscription> findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(String userEmail, String planCode);

    List<Subscription> findByStatusAndRenewalReminderSentFalse(SubscriptionStatus status);
}
