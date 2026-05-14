package com.connectsphere.payment.service;

import java.util.List;
import java.util.Map;

import com.connectsphere.payment.dto.PaymentConfirmRequestDto;
import com.connectsphere.payment.dto.PaymentRequestDto;
import com.connectsphere.payment.dto.PaymentResponseDto;
import com.connectsphere.payment.dto.PaymentWebhookRequestDto;
import com.connectsphere.payment.dto.SubscriptionActionRequestDto;
import com.connectsphere.payment.entity.Payment;

/**
 * <h1>PaymentService Interface</h1>
 * <p>Handles the monetization and subscription economy of the ConnectSphere platform.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>Transaction Orchestration:</b> Managing the lifecycle of payments from intent creation to final confirmation.</li>
 *     <li><b>Subscription Management:</b> Handling renewals, upgrades, and cancellations for premium tiers.</li>
 *     <li><b>Security:</b> Validating webhook signatures and provider-specific payment verification.</li>
 *     <li><b>Reporting:</b> Generating PDF receipts and maintaining an audit trail of all financial activities.</li>
 * </ul>
 * 
 * <h2>Monetization Flow:</h2>
 * <pre>
 * graph TD
 *     A[User] -->|Purchase| B{Create Intent}
 *     B -->|Sandbox/Prod| C[Provider Gateway]
 *     C -->|Confirm| D{Verification}
 *     D -- Success --> E[Activate Premium/Badge]
 *     D -- Fail --> F[Mark Failed]
 *     E --> G[(Audit Log)]
 *     E --> H[Issue Receipt PDF]
 * </pre>
 */
public interface PaymentService {

    PaymentResponseDto createPaymentIntent(String userEmail, PaymentRequestDto request);

    PaymentResponseDto confirmPayment(String userEmail, PaymentConfirmRequestDto request);

    List<Payment> getPaymentsByUser(String userEmail);

    List<Payment> getAllPayments();

    Payment getPaymentById(Long paymentId);

    List<Map<String, Object>> getMySubscriptions(String userEmail);

    Map<String, Object> getCurrentSubscription(String userEmail, String planCode);

    Map<String, Object> renewSubscription(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request);

    Map<String, Object> cancelSubscription(String userEmail, Long subscriptionId);

    Map<String, Object> setAutoRenew(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request);

    Map<String, Object> upgradeSubscription(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request);

    Map<String, Object> getReceipt(String userEmail, Long paymentId);

    byte[] downloadReceiptPdf(String userEmail, Long paymentId);


    List<Map<String, Object>> getDueRenewalReminders();

    Map<String, Object> handleWebhook(PaymentWebhookRequestDto request, String signature);
}
