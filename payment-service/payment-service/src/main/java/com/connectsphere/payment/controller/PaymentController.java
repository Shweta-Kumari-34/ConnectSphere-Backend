package com.connectsphere.payment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.payment.dto.PaymentConfirmRequestDto;
import com.connectsphere.payment.dto.PaymentRequestDto;
import com.connectsphere.payment.dto.PaymentResponseDto;
import com.connectsphere.payment.dto.PaymentWebhookRequestDto;
import com.connectsphere.payment.dto.SubscriptionActionRequestDto;
import com.connectsphere.payment.entity.Payment;
import com.connectsphere.payment.service.PaymentService;

import jakarta.validation.Valid;

/**
 * REST controller managing payment intents, transaction confirmations, and subscriptions.
 * <p>
 * Acts as the primary entry point for all monetization features, including
 * purchasing premium memberships and verified badges.
 * </p>
 *
 * <h3>Controller Architecture</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Client Request] --> B[PaymentController];
 *     B --> C[PaymentService];
 *     C --> D[(MySQL - Payments)];
 * </pre>
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    // Payment orchestration and provider integrations are handled by PaymentService.
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public ResponseEntity<PaymentResponseDto> createIntent(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody PaymentRequestDto request) {
        // Creates provider-ready intent/order and stores pending payment record.
        return ResponseEntity.ok(paymentService.createPaymentIntent(userEmail, request));
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDto> processLegacyPayment(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody PaymentRequestDto request) {
        // Backward-compatible single-step flow: create intent + auto-confirm.
        PaymentResponseDto intent = paymentService.createPaymentIntent(userEmail, request);
        PaymentConfirmRequestDto confirmRequest = new PaymentConfirmRequestDto();
        confirmRequest.setPaymentId(intent.getPaymentId());
        confirmRequest.setSuccess(true);
        confirmRequest.setProviderPaymentId("LEGACY-" + intent.getTransactionId());
        return ResponseEntity.ok(paymentService.confirmPayment(userEmail, confirmRequest));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponseDto> confirmPayment(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody PaymentConfirmRequestDto request) {
        // Finalizes payment status and applies premium/verification side effects.
        return ResponseEntity.ok(paymentService.confirmPayment(userEmail, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userEmail));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/subscriptions/my")
    public ResponseEntity<List<Map<String, Object>>> getMySubscriptions(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getMySubscriptions(userEmail));
    }

    @GetMapping("/subscriptions/current")
    public ResponseEntity<Map<String, Object>> getCurrentSubscription(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam("planCode") String planCode) {
        return ResponseEntity.ok(paymentService.getCurrentSubscription(userEmail, planCode));
    }

    @PostMapping("/subscriptions/{id}/renew")
    public ResponseEntity<Map<String, Object>> renewSubscription(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable("id") Long id,
            @RequestBody(required = false) SubscriptionActionRequestDto request) {
        return ResponseEntity.ok(paymentService.renewSubscription(userEmail, id, request == null ? new SubscriptionActionRequestDto() : request));
    }

    @PutMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSubscription(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(paymentService.cancelSubscription(userEmail, id));
    }

    @PutMapping("/subscriptions/{id}/auto-renew")
    public ResponseEntity<Map<String, Object>> setAutoRenew(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable("id") Long id,
            @RequestBody SubscriptionActionRequestDto request) {
        return ResponseEntity.ok(paymentService.setAutoRenew(userEmail, id, request));
    }

    @PostMapping("/subscriptions/{id}/upgrade")
    public ResponseEntity<Map<String, Object>> upgradeSubscription(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable("id") Long id,
            @RequestBody SubscriptionActionRequestDto request) {
        return ResponseEntity.ok(paymentService.upgradeSubscription(userEmail, id, request));
    }

    @GetMapping("/receipts/{paymentId}")
    public ResponseEntity<Map<String, Object>> getReceipt(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getReceipt(userEmail, paymentId));
    }

    @GetMapping("/receipts/{paymentId}/download")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable Long paymentId) {
        byte[] pdfBytes = paymentService.downloadReceiptPdf(userEmail, paymentId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ConnectSphere_Receipt_" + paymentId + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }


    @GetMapping("/renewal-reminders/due")
    public ResponseEntity<List<Map<String, Object>>> getDueRenewalReminders() {
        return ResponseEntity.ok(paymentService.getDueRenewalReminders());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody PaymentWebhookRequestDto request) {
        // Provider callback endpoint for asynchronous payment events.
        return ResponseEntity.ok(paymentService.handleWebhook(request, signature));
    }

    @GetMapping("/test")
    public String test() {
        return "Payment Service is running";
    }
}
