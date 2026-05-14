package com.connectsphere.payment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.connectsphere.payment.dto.PaymentConfirmRequestDto;
import com.connectsphere.payment.dto.PaymentRequestDto;
import com.connectsphere.payment.dto.PaymentResponseDto;
import com.connectsphere.payment.dto.PaymentWebhookRequestDto;
import com.connectsphere.payment.dto.SubscriptionActionRequestDto;
import com.connectsphere.payment.entity.Payment;
import com.connectsphere.payment.entity.Payment.PaymentStatus;
import com.connectsphere.payment.entity.Subscription;
import com.connectsphere.payment.entity.Subscription.SubscriptionStatus;
import com.connectsphere.payment.exception.BadRequestException;
import com.connectsphere.payment.exception.ResourceNotFoundException;
import com.connectsphere.payment.repository.PaymentRepository;
import com.connectsphere.payment.repository.SubscriptionRepository;
import com.connectsphere.payment.service.EmailService;
import com.connectsphere.payment.service.PaymentService;
import com.connectsphere.payment.util.PaymentConstants;

/**
 * <h1>PaymentServiceImpl</h1>
 * <p>Comprehensive implementation of {@link PaymentService} that bridges financial gateways 
 * with the platform's core identity and access management system.</p>
 * 
 * <h2>Internal Synchronization Flow:</h2>
 * <pre>
 * sequenceDiagram
 *     Actor->>PaymentService: confirmPayment(ID)
 *     PaymentService->>Gateway: Verify Transaction (Razorpay/UPI)
 *     Gateway-->>PaymentService: Success Response
 *     PaymentService->>AuthService: Sync Subscription (Internal API Call)
 *     AuthService-->>PaymentService: Sync OK
 *     PaymentService->>EmailService: Dispatch Receipt PDF
 *     PaymentService->>DB: Save Subscription & Payment Records
 * </pre>
 * 
 * <h2>Technical Architecture:</h2>
 * <ul>
 *     <li><b>Gateway Decoupling:</b> Supports both Razorpay (Sandbox/Live) and a custom Demo UPI QR generator.</li>
 *     <li><b>Atomic Sync:</b> Uses internal REST calls to {@code auth-service} to ensure subscription benefits 
 *         are immediately available to the user.</li>
 *     <li><b>Receipt Generation:</b> Implements {@code iText} for on-the-fly PDF generation of transaction receipts.</li>
 *     <li><b>Scheduled Tasks:</b> Includes background logic for identifying subscriptions due for renewal.</li>
 * </ul>
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final BigDecimal VERIFIED_BADGE_PRICE = new BigDecimal("699.00");
    private static final BigDecimal PREMIUM_PRICE = new BigDecimal("999.00");

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Value("${connectsphere.auth-service.url:http://localhost:8083}")
    private String authServiceUrl;

    @Value("${connectsphere.notification-service.url:http://localhost:8086}")
    private String notificationServiceUrl;

    @Value("${connectsphere.internal-secret:connectsphere-internal-secret}")
    private String internalSecret;

    @Value("${connectsphere.webhook-secret:connectsphere-webhook-secret}")
    private String webhookSecret;

    @Value("${connectsphere.demo-upi.virtual-address:demo.connectsphere@okaxis}")
    private String demoUpiVirtualAddress;

    @Value("${connectsphere.demo-upi.merchant-name:ConnectSphere Demo Merchant}")
    private String demoUpiMerchantName;

    @Value("${connectsphere.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${connectsphere.razorpay.sandbox-mode:true}")
    private boolean razorpaySandboxMode;

    @Value("${connectsphere.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${connectsphere.razorpay.key-secret:}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              SubscriptionRepository subscriptionRepository,
                              RestTemplateBuilder restTemplateBuilder,
                              EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplateBuilder.build();
        this.emailService = emailService;
    }

    @Override
    public PaymentResponseDto createPaymentIntent(String userEmail, PaymentRequestDto request) {
        String normalizedPlan = normalizePlanCode(request.getPlanCode());
        String provider = normalizeProvider(request.getPaymentProvider());
        String paymentMethod = normalizeText(request.getPaymentMethod());

        if (PaymentConstants.PLAN_VERIFIED.equals(normalizedPlan)) {
            validateVerificationApproval(userEmail);
        }

        BigDecimal amount = resolveAmount(normalizedPlan, request.getAmount());
        Payment payment = new Payment();
        payment.setUserEmail(userEmail);
        payment.setPlanCode(normalizedPlan);
        payment.setAmount(amount);
        payment.setCurrency(PaymentConstants.CURRENCY_INR);
        payment.setDescription(request.getDescription() == null || request.getDescription().isBlank()
                ? defaultDescription(normalizedPlan)
                : request.getDescription().trim());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentProvider(provider);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        payment.setProviderOrderId(createProviderOrderId(provider));
        if (PaymentConstants.PROVIDER_RAZORPAY.equals(provider)) {
            payment.setProviderOrderId(createRazorpayOrderId(payment));
        }
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        payment.setMetadataJson(buildMetadataJson(request));

        Payment saved = paymentRepository.save(payment);
        return toIntentResponse(saved);
    }

    @Override
    public PaymentResponseDto confirmPayment(String userEmail, PaymentConfirmRequestDto request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("This payment does not belong to your account");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is already finalized");
        }
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            throw new BadRequestException("Payment intent has expired. Please retry.");
        }

        if (!request.isSuccess()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return toFinalResponse(payment, "Payment marked as failed");
        }

        if (PaymentConstants.PROVIDER_RAZORPAY.equals(payment.getPaymentProvider())) {
            verifyRazorpaySignature(payment, request);
        }

        payment.setProviderPaymentId(resolveProviderPaymentId(payment, request));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setConfirmedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setReceiptNumber("RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        Payment savedPayment = paymentRepository.save(payment);

        activateBenefitsAndCommunicate(savedPayment, extractAutoRenew(savedPayment), extractTheme(savedPayment));
        return toFinalResponse(savedPayment, buildSuccessMessage(savedPayment.getPlanCode()));
    }

    @Override
    public List<Payment> getPaymentsByUser(String userEmail) {
        return paymentRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
    }

    @Override
    public List<Map<String, Object>> getMySubscriptions(String userEmail) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserEmailOrderByStartedAtDesc(userEmail);
        subscriptions.forEach(this::refreshStatusIfExpired);
        return subscriptions.stream()
                .sorted(Comparator.comparing(Subscription::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSubscriptionMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCurrentSubscription(String userEmail, String planCode) {
        String normalized = normalizePlanCode(planCode);
        Subscription subscription = subscriptionRepository.findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(userEmail, normalized)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for this plan"));
        refreshStatusIfExpired(subscription);
        return toSubscriptionMap(subscription);
    }

    @Override
    public Map<String, Object> renewSubscription(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request) {
        Subscription subscription = getOwnedSubscription(userEmail, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Cancelled subscriptions cannot be renewed directly. Purchase again.");
        }

        Payment renewalPayment = new Payment();
        renewalPayment.setUserEmail(userEmail);
        renewalPayment.setPlanCode(subscription.getPlanCode());
        renewalPayment.setAmount(resolveAmount(subscription.getPlanCode(), null));
        renewalPayment.setCurrency(PaymentConstants.CURRENCY_INR);
        renewalPayment.setDescription("Renewal for " + subscription.getPlanCode());
        renewalPayment.setPaymentMethod(PaymentConstants.METHOD_UPI);
        renewalPayment.setPaymentProvider(PaymentConstants.PROVIDER_UPI_QR);
        renewalPayment.setStatus(PaymentStatus.SUCCESS);
        renewalPayment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        renewalPayment.setProviderOrderId(createProviderOrderId(PaymentConstants.PROVIDER_UPI_QR));
        renewalPayment.setProviderPaymentId("AUTO-RENEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        renewalPayment.setReceiptNumber("RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        renewalPayment.setCreatedAt(LocalDateTime.now());
        renewalPayment.setUpdatedAt(LocalDateTime.now());
        renewalPayment.setConfirmedAt(LocalDateTime.now());
        renewalPayment.setMetadataJson("{\"autoRenew\":" + subscription.isAutoRenew() + ",\"theme\":\"" + normalizeTheme(request.getTheme()) + "\"}");
        Payment savedRenewalPayment = paymentRepository.save(renewalPayment);

        Subscription renewed = activateBenefitsAndCommunicate(savedRenewalPayment, subscription.isAutoRenew(), request.getTheme());
        Map<String, Object> response = toSubscriptionMap(renewed);
        response.put("message", "Subscription renewed successfully");
        return response;
    }

    @Override
    public Map<String, Object> cancelSubscription(String userEmail, Long subscriptionId) {
        Subscription subscription = getOwnedSubscription(userEmail, subscriptionId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscription.setCancelledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        pushSubscriptionToAuth(userEmail, subscription, "subscription-" + subscription.getId());

        Map<String, Object> response = toSubscriptionMap(subscription);
        response.put("message", "Subscription cancelled");
        return response;
    }

    @Override
    public Map<String, Object> setAutoRenew(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request) {
        Subscription subscription = getOwnedSubscription(userEmail, subscriptionId);
        subscription.setAutoRenew(request.isAutoRenew());
        subscriptionRepository.save(subscription);
        pushSubscriptionToAuth(userEmail, subscription, "subscription-" + subscription.getId());

        Map<String, Object> response = toSubscriptionMap(subscription);
        response.put("message", request.isAutoRenew() ? "Auto-renew enabled" : "Auto-renew disabled");
        return response;
    }

    @Override
    public Map<String, Object> upgradeSubscription(String userEmail, Long subscriptionId, SubscriptionActionRequestDto request) {
        Subscription current = getOwnedSubscription(userEmail, subscriptionId);
        if (!PaymentConstants.PLAN_VERIFIED.equals(current.getPlanCode())) {
            throw new BadRequestException("Only Verified Badge plan can be upgraded from this endpoint");
        }
        if (!PaymentConstants.PLAN_PREMIUM.equalsIgnoreCase(normalizeText(request.getTargetPlanCode()))) {
            throw new BadRequestException("Only upgrade path currently supported: PREMIUM_MEMBERSHIP");
        }

        PaymentRequestDto intentRequest = new PaymentRequestDto();
        intentRequest.setPlanCode(PaymentConstants.PLAN_PREMIUM);
        intentRequest.setPaymentProvider(PaymentConstants.PROVIDER_UPI_QR);
        intentRequest.setPaymentMethod(PaymentConstants.METHOD_UPI);
        intentRequest.setAutoRenew(true);
        intentRequest.setTheme(request.getTheme());

        PaymentResponseDto intent = createPaymentIntent(userEmail, intentRequest);
        PaymentConfirmRequestDto confirm = new PaymentConfirmRequestDto();
        confirm.setPaymentId(intent.getPaymentId());
        confirm.setSuccess(true);
        confirm.setProviderPaymentId("UPGRADE-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        confirmPayment(userEmail, confirm);

        Subscription upgraded = subscriptionRepository.findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(userEmail, PaymentConstants.PLAN_PREMIUM)
                .orElseThrow(() -> new ResourceNotFoundException("Upgraded subscription not found"));
        Map<String, Object> response = toSubscriptionMap(upgraded);
        response.put("message", "Upgraded to Premium Membership");
        return response;
    }

    @Override
    public Map<String, Object> getReceipt(String userEmail, Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (!payment.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Receipt unavailable for this payment");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new BadRequestException("Receipt is available only for successful payments");
        }
        return Map.of(
                "receiptNumber", payment.getReceiptNumber(),
                "transactionId", payment.getTransactionId(),
                "paymentId", payment.getPaymentId(),
                "planCode", payment.getPlanCode(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "paidAt", payment.getConfirmedAt(),
                "paymentMethod", payment.getPaymentMethod(),
                "paymentProvider", payment.getPaymentProvider());
    }

    @Override
    public byte[] downloadReceiptPdf(String userEmail, Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (!payment.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Receipt unavailable for this payment");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new BadRequestException("Receipt is available only for successful payments");
        }

        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.NORMAL);
            com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);

            document.add(new com.itextpdf.text.Paragraph("ConnectSphere", titleFont));
            document.add(new com.itextpdf.text.Paragraph("Payment Receipt", titleFont));
            document.add(new com.itextpdf.text.Paragraph(" "));

            document.add(new com.itextpdf.text.Paragraph("Receipt Number: " + payment.getReceiptNumber(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Transaction ID: " + payment.getTransactionId(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Date: " + payment.getConfirmedAt(), normalFont));
            document.add(new com.itextpdf.text.Paragraph(" "));

            document.add(new com.itextpdf.text.Paragraph("Customer: " + payment.getUserEmail(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Plan: " + payment.getPlanCode(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Payment Provider: " + payment.getPaymentProvider(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Payment Status: " + payment.getStatus(), normalFont));
            document.add(new com.itextpdf.text.Paragraph(" "));

            document.add(new com.itextpdf.text.Paragraph("Amount Paid: " + payment.getCurrency() + " " + payment.getAmount(), boldFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate PDF receipt");
        }
    }

    @Override
    public List<Map<String, Object>> getDueRenewalReminders() {
        LocalDateTime reminderWindow = LocalDateTime.now().plusDays(3);
        List<Subscription> active = subscriptionRepository.findByStatusAndRenewalReminderSentFalse(SubscriptionStatus.ACTIVE);
        List<Map<String, Object>> reminders = new java.util.ArrayList<>();
        for (Subscription sub : active) {
            if (sub.getExpiresAt() == null || !sub.getExpiresAt().isBefore(reminderWindow)) {
                continue;
            }
            sub.setRenewalReminderSent(true);
            subscriptionRepository.save(sub);

            Map<String, Object> reminder = new HashMap<>();
            reminder.put("subscriptionId", sub.getId());
            reminder.put("userEmail", sub.getUserEmail());
            reminder.put("planCode", sub.getPlanCode());
            reminder.put("expiresAt", sub.getExpiresAt());
            reminder.put("message", "Your subscription expires soon. Renew to keep benefits active.");
            reminders.add(reminder);
        }
        return reminders;
    }

    @Override
    public Map<String, Object> handleWebhook(PaymentWebhookRequestDto request, String signature) {
        validateWebhookSignature(request, signature);

        Payment payment = resolveWebhookPayment(request);
        if (payment == null) {
            return Map.of("status", "IGNORED", "message", "No matching payment found");
        }

        String status = normalizeText(request.getStatus());
        if ("SUCCESS".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                PaymentConfirmRequestDto confirmRequest = new PaymentConfirmRequestDto();
                confirmRequest.setPaymentId(payment.getPaymentId());
                confirmRequest.setProviderPaymentId(request.getProviderPaymentId());
                confirmRequest.setSuccess(true);
                confirmPayment(payment.getUserEmail(), confirmRequest);
            }
            return Map.of("status", "OK", "message", "Payment confirmed from webhook");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return Map.of("status", "OK", "message", "Payment marked failed from webhook");
    }

    private Subscription getOwnedSubscription(String userEmail, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (!subscription.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Subscription does not belong to this user");
        }
        refreshStatusIfExpired(subscription);
        return subscription;
    }

    private void refreshStatusIfExpired(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getExpiresAt() != null
                && subscription.getExpiresAt().isBefore(LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            pushSubscriptionToAuth(subscription.getUserEmail(), subscription, "subscription-" + subscription.getId());
        }
    }

    private Subscription activateSubscription(Payment payment, boolean active, boolean autoRenew, String theme) {
        String planCode = payment.getPlanCode();
        Subscription subscription = subscriptionRepository.findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(
                        payment.getUserEmail(), planCode)
                .orElse(new Subscription());
        Map<?, ?> profile = fetchUserProfile(payment.getUserEmail());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)
                ? subscription.getExpiresAt()
                : now;
        LocalDateTime nextExpiry = PaymentConstants.PLAN_PREMIUM.equals(planCode) ? base.plusDays(30) : base.plusDays(365);

        subscription.setUserId(resolveUserId(profile, subscription.getUserId(), payment.getUserEmail()));
        subscription.setUserEmail(payment.getUserEmail());
        subscription.setPlanId(resolvePlanId(planCode));
        subscription.setPlanCode(planCode);
        subscription.setStatus(active ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED);
        subscription.setStartedAt(subscription.getStartedAt() == null ? now : subscription.getStartedAt());
        subscription.setStartDate(subscription.getStartedAt().toLocalDate());
        subscription.setLastRenewedAt(now);
        subscription.setExpiresAt(nextExpiry);
        subscription.setExpiryDate(nextExpiry.toLocalDate());
        subscription.setAutoRenew(autoRenew);
        subscription.setFeatureSnapshot(buildFeatureSnapshot(planCode, theme));
        return subscriptionRepository.save(subscription);
    }

    private Subscription activateBenefitsAndCommunicate(Payment payment, boolean autoRenew, String theme) {
        Subscription subscription = activateSubscription(payment, true, autoRenew, theme);
        pushSubscriptionToAuth(payment.getUserEmail(), subscription, paymentReference(payment));
        sendPaymentSuccessNotification(payment);
        sendReceiptEmail(payment);
        return subscription;
    }

    private void pushSubscriptionToAuth(String userEmail, Subscription subscription, String sourcePaymentReference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalSecret);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", userEmail);
            payload.put("planCode", subscription.getPlanCode());
            payload.put("active", subscription.getStatus() == SubscriptionStatus.ACTIVE);
            payload.put("expiresAt", subscription.getExpiresAt());
            payload.put("autoRenew", subscription.isAutoRenew());
            payload.put("theme", extractThemeFromFeatureSnapshot(subscription.getFeatureSnapshot()));
            payload.put("sourcePaymentReference", sourcePaymentReference);

            restTemplate.exchange(
                    authServiceUrl + "/auth/internal/subscription-activation",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
        } catch (Exception ex) {
            log.warn("Unable to sync subscription to auth-service for {}: {}", userEmail, ex.getMessage());
        }
    }

    private void validateVerificationApproval(String userEmail) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Email", userEmail);
            Map<?, ?> verification = restTemplate.exchange(
                    authServiceUrl + "/auth/verification/my-request",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            Object status = verification == null ? null : verification.get("status");
            if (status == null || !("PAYMENT_PENDING".equalsIgnoreCase(status.toString())
                    || "APPROVED".equalsIgnoreCase(status.toString()))) {
                throw new BadRequestException("Verification application must be approved before payment");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to validate verification request status");
        }
    }

    private PaymentResponseDto toIntentResponse(Payment payment) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(payment.getPaymentId());
        response.setMessage("Payment intent created");
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPlanCode(payment.getPlanCode());
        response.setStatus(payment.getStatus().name());
        response.setTransactionId(payment.getTransactionId());
        response.setPaymentProvider(payment.getPaymentProvider());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setProviderOrderId(payment.getProviderOrderId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setExpiresAt(payment.getExpiresAt());

        if (PaymentConstants.PROVIDER_UPI_QR.equals(payment.getPaymentProvider())) {
            String upiPayload = buildDemoUpiPayload(payment);
            response.setUpiQrPayload(upiPayload);
            response.setUpiQrImageUrl(buildDemoQrImage(payment, upiPayload));
            response.setMessage("Demo UPI QR ready. No real bank account is required. Scan or review the payload, then click Confirm Payment.");
        } else if (PaymentConstants.PROVIDER_RAZORPAY.equals(payment.getPaymentProvider())) {
            response.setRazorpayOrderId(payment.getProviderOrderId());
            response.setRazorpayKeyId(resolveRazorpayPublicKey());
            response.setMessage("Razorpay sandbox ready. No real money will be charged. Click Confirm Payment to simulate success or Mark Failed to test failure.");
        }

        return response;
    }

    private PaymentResponseDto toFinalResponse(Payment payment, String message) {
        PaymentResponseDto response = toIntentResponse(payment);
        response.setMessage(message);
        response.setStatus(payment.getStatus().name());
        response.setReceiptNumber(payment.getReceiptNumber());
        response.setConfirmedAt(payment.getConfirmedAt());
        return response;
    }

    private Map<String, Object> toSubscriptionMap(Subscription subscription) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", subscription.getId());
        map.put("userId", subscription.getUserId());
        map.put("userEmail", subscription.getUserEmail());
        map.put("planId", subscription.getPlanId());
        map.put("planCode", subscription.getPlanCode());
        map.put("status", subscription.getStatus().name());
        map.put("startedAt", subscription.getStartedAt());
        map.put("startDate", subscription.getStartDate());
        map.put("expiresAt", subscription.getExpiresAt());
        map.put("expiryDate", subscription.getExpiryDate());
        map.put("autoRenew", subscription.isAutoRenew());
        map.put("lastRenewedAt", subscription.getLastRenewedAt());
        map.put("cancelledAt", subscription.getCancelledAt());
        map.put("featureSnapshot", subscription.getFeatureSnapshot());
        return map;
    }

    private BigDecimal resolveAmount(String planCode, BigDecimal requestedAmount) {
        if (PaymentConstants.PLAN_VERIFIED.equals(planCode)) {
            return VERIFIED_BADGE_PRICE;
        }
        if (PaymentConstants.PLAN_PREMIUM.equals(planCode)) {
            return PREMIUM_PRICE;
        }
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ONE) < 0) {
            throw new BadRequestException("Amount must be provided for custom payments");
        }
        return requestedAmount;
    }

    private String defaultDescription(String planCode) {
        if (PaymentConstants.PLAN_VERIFIED.equals(planCode)) {
            return "ConnectSphere Verified Badge Plan";
        }
        if (PaymentConstants.PLAN_PREMIUM.equals(planCode)) {
            return "ConnectSphere Premium Membership";
        }
        return "ConnectSphere Payment";
    }

    private String normalizePlanCode(String planCode) {
        String normalized = normalizeText(planCode).toUpperCase();
        if (!PaymentConstants.PLAN_VERIFIED.equals(normalized) && !PaymentConstants.PLAN_PREMIUM.equals(normalized)) {
            throw new BadRequestException("Unsupported plan code");
        }
        return normalized;
    }

    private String normalizeProvider(String provider) {
        String normalized = normalizeText(provider).toUpperCase();
        if (!List.of(
                PaymentConstants.PROVIDER_UPI_QR,
                PaymentConstants.PROVIDER_RAZORPAY).contains(normalized)) {
            throw new BadRequestException("Unsupported payment provider");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String createProviderOrderId(String provider) {
        String prefix = "ord";
        if (PaymentConstants.PROVIDER_RAZORPAY.equals(provider)) {
            prefix = "rzp_order";
        }
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String createRazorpayOrderId(Payment payment) {
        if (!isRazorpayReady()) {
            return "rzp_demo_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String basic = Base64.getEncoder().encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + basic);

            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", payment.getAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact());
            payload.put("currency", payment.getCurrency());
            payload.put("receipt", payment.getTransactionId());
            payload.put("notes", Map.of(
                    "planCode", payment.getPlanCode(),
                    "userEmail", payment.getUserEmail()
            ));

            Map<?, ?> response = restTemplate.exchange(
                    "https://api.razorpay.com/v1/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Map.class
            ).getBody();

            String orderId = response == null ? "" : normalizeText(String.valueOf(response.get("id")));
            if (orderId.isEmpty()) {
                throw new BadRequestException("Unable to create Razorpay order");
            }
            return orderId;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Razorpay order creation failed: {}", ex.getMessage());
            throw new BadRequestException("Razorpay checkout unavailable right now. Please retry.");
        }
    }

    private boolean isRazorpayReady() {
        return razorpayEnabled
                && !normalizeText(razorpayKeyId).isEmpty()
                && !normalizeText(razorpayKeySecret).isEmpty();
    }

    private String resolveRazorpayPublicKey() {
        String key = normalizeText(razorpayKeyId);
        return key.isEmpty() ? "rzp_test_connectsphere_demo" : key;
    }

    private String buildMetadataJson(PaymentRequestDto request) {
        String theme = normalizeTheme(request.getTheme());
        return "{\"autoRenew\":" + request.isAutoRenew() + ",\"theme\":\"" + theme + "\"}";
    }

    private boolean extractAutoRenew(Payment payment) {
        String metadata = payment.getMetadataJson();
        return metadata != null && metadata.contains("\"autoRenew\":true");
    }

    private String extractTheme(Payment payment) {
        String metadata = payment.getMetadataJson();
        if (metadata == null || !metadata.contains("\"theme\"")) {
            return PaymentConstants.THEME_CLASSIC;
        }
        int idx = metadata.indexOf("\"theme\":\"");
        if (idx < 0) {
            return PaymentConstants.THEME_CLASSIC;
        }
        int start = idx + "\"theme\":\"".length();
        int end = metadata.indexOf("\"", start);
        if (end < start) {
            return PaymentConstants.THEME_CLASSIC;
        }
        return metadata.substring(start, end);
    }

    private String buildFeatureSnapshot(String planCode, String theme) {
        String normalizedTheme = normalizeTheme(theme);
        if (PaymentConstants.PLAN_VERIFIED.equals(planCode)) {
            return "{\"badge\":true,\"prioritySearch\":true,\"theme\":\"" + normalizedTheme + "\"}";
        }
        return "{\"profileBoost\":true,\"analytics\":true,\"prioritySupport\":true,"
                + "\"exclusiveThemes\":true,\"advancedPrivacy\":true,\"searchBoost\":true,"
                + "\"theme\":\"" + normalizedTheme + "\"}";
    }

    private String extractThemeFromFeatureSnapshot(String snapshot) {
        if (snapshot == null) {
            return PaymentConstants.THEME_CLASSIC;
        }
        int idx = snapshot.indexOf("\"theme\":\"");
        if (idx < 0) {
            return PaymentConstants.THEME_CLASSIC;
        }
        int start = idx + "\"theme\":\"".length();
        int end = snapshot.indexOf("\"", start);
        if (end < start) {
            return PaymentConstants.THEME_CLASSIC;
        }
        return snapshot.substring(start, end);
    }

    private void validateWebhookSignature(PaymentWebhookRequestDto request, String signature) {
        if (signature == null || signature.isBlank()) {
            throw new BadRequestException("Missing webhook signature");
        }
        String payload = String.valueOf(request.getPaymentId()) + ":" + normalizeText(request.getEventType())
                + ":" + normalizeText(request.getStatus()) + ":" + request.getTimestamp();

        String computed = hmacSha256(payload, webhookSecret);
        if (!computed.equalsIgnoreCase(signature.trim())) {
            throw new BadRequestException("Invalid webhook signature");
        }
    }

    private Payment resolveWebhookPayment(PaymentWebhookRequestDto request) {
        if (request.getPaymentId() != null) {
            return paymentRepository.findById(request.getPaymentId()).orElse(null);
        }
        if (request.getTransactionId() != null && !request.getTransactionId().isBlank()) {
            return paymentRepository.findByTransactionId(request.getTransactionId()).orElse(null);
        }
        return null;
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify webhook signature");
        }
    }

    private String buildDemoUpiPayload(Payment payment) {
        String amount = payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return "upi://pay?pa=" + urlEncode(demoUpiVirtualAddress)
                + "&pn=" + urlEncode(demoUpiMerchantName)
                + "&am=" + amount
                + "&cu=" + PaymentConstants.CURRENCY_INR
                + "&tr=" + urlEncode(payment.getTransactionId())
                + "&tn=" + urlEncode("ConnectSphere demo payment for " + humanPlanName(payment.getPlanCode()))
                + "&mc=5814";
    }

    private String buildDemoQrImage(Payment payment, String upiPayload) {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="360" height="420" viewBox="0 0 360 420">
                  <rect width="360" height="420" rx="24" fill="#0f172a"/>
                  <rect x="24" y="24" width="312" height="372" rx="18" fill="#ffffff"/>
                  <rect x="52" y="84" width="256" height="256" rx="12" fill="#e2e8f0"/>
                  <text x="180" y="126" text-anchor="middle" font-family="Arial" font-size="18" font-weight="700" fill="#0f172a">DEMO QR</text>
                  <text x="180" y="154" text-anchor="middle" font-family="Arial" font-size="11" fill="#475569">No real bank account required</text>
                  <text x="180" y="196" text-anchor="middle" font-family="monospace" font-size="12" fill="#0f172a">PLAN: %s</text>
                  <text x="180" y="220" text-anchor="middle" font-family="monospace" font-size="12" fill="#0f172a">AMOUNT: %s %s</text>
                  <text x="180" y="244" text-anchor="middle" font-family="monospace" font-size="12" fill="#0f172a">TXN: %s</text>
                  <text x="180" y="310" text-anchor="middle" font-family="Arial" font-size="10" fill="#64748b">Use Confirm Payment to simulate success</text>
                  <text x="180" y="352" text-anchor="middle" font-family="Arial" font-size="11" font-weight="700" fill="#2563eb">Demo UPI payload attached</text>
                  <text x="180" y="374" text-anchor="middle" font-family="Arial" font-size="9" fill="#64748b">%s</text>
                </svg>
                """.formatted(
                humanPlanName(payment.getPlanCode()),
                payment.getCurrency(),
                payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                payment.getTransactionId(),
                escapeXml(upiPayload));
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private void sendPaymentSuccessNotification(Payment payment) {
        try {
            String notificationType = PaymentConstants.PLAN_VERIFIED.equals(payment.getPlanCode())
                    ? "VERIFICATION_PAYMENT_SUCCESS"
                    : "PREMIUM_SUBSCRIPTION_SUCCESS";
            String message = PaymentConstants.PLAN_VERIFIED.equals(payment.getPlanCode())
                    ? "Your verified badge payment was confirmed. The badge is now active on your profile."
                    : "Your premium membership payment was confirmed. Premium benefits are now active.";

            String url = UriComponentsBuilder
                    .fromHttpUrl(notificationServiceUrl + "/notifications")
                    .queryParam("recipientEmail", payment.getUserEmail())
                    .queryParam("senderEmail", PaymentConstants.SYSTEM_SENDER)
                    .queryParam("type", notificationType)
                    .queryParam("message", message)
                    .queryParam("referenceId", payment.getPaymentId())
                    .queryParam("actionUrl", "/payments")
                    .queryParam("referenceType", "PAYMENT")
                    .build()
                    .encode()
                    .toUriString();

            restTemplate.postForEntity(url, HttpEntity.EMPTY, Object.class);
        } catch (Exception ex) {
            log.warn("Unable to send payment notification for {}: {}", payment.getUserEmail(), ex.getMessage());
        }
    }

    private void sendReceiptEmail(Payment payment) {
        Map<?, ?> profile = fetchUserProfile(payment.getUserEmail());
        Object fullName = profile == null ? null : profile.get("fullName");
        String displayName = profile == null
                ? payment.getUserEmail().split("@")[0]
                : normalizeText(fullName == null ? payment.getUserEmail().split("@")[0] : String.valueOf(fullName));

        String body = "Hi " + displayName + ",\n\n"
                + "Your ConnectSphere demo payment was completed successfully.\n\n"
                + "Receipt Number: " + payment.getReceiptNumber() + "\n"
                + "Transaction ID: " + payment.getTransactionId() + "\n"
                + "Plan: " + humanPlanName(payment.getPlanCode()) + "\n"
                + "Amount: " + payment.getCurrency() + " " + payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + "\n"
                + "Confirmed At: " + payment.getConfirmedAt() + "\n"
                + "Payment Mode: " + payment.getPaymentMethod() + " / " + payment.getPaymentProvider() + "\n\n"
                + "This is a realistic demo payment flow. No real money was charged and no real bank account was required.\n\n"
                + "You can also download the receipt from the Payments page.";

        emailService.sendEmail(
                payment.getUserEmail(),
                "ConnectSphere Payment Receipt - " + payment.getReceiptNumber(),
                body
        );
    }

    private Map<?, ?> fetchUserProfile(String userEmail) {
        try {
            return restTemplate.getForObject(authServiceUrl + "/auth/user/{email}", Map.class, userEmail);
        } catch (Exception ex) {
            log.debug("Unable to load user profile for {}: {}", userEmail, ex.getMessage());
            return null;
        }
    }

    private Long resolveUserId(Map<?, ?> profile, Long existingUserId, String userEmail) {
        if (existingUserId != null) {
            return existingUserId;
        }
        if (profile != null) {
            Object userId = profile.get("userId");
            if (userId instanceof Number number) {
                return number.longValue();
            }
            if (userId != null) {
                try {
                    return Long.parseLong(userId.toString());
                } catch (NumberFormatException ex) {
                    log.warn("Unable to parse userId '{}' for {}", userId, userEmail);
                }
            }
        }
        throw new BadRequestException("Unable to resolve user identity for subscription activation");
    }

    private Long resolvePlanId(String planCode) {
        if (PaymentConstants.PLAN_VERIFIED.equals(planCode)) {
            return 1L;
        }
        if (PaymentConstants.PLAN_PREMIUM.equals(planCode)) {
            return 2L;
        }
        throw new BadRequestException("Unsupported plan code");
    }

    private String resolveProviderPaymentId(Payment payment, PaymentConfirmRequestDto request) {
        String provided = normalizeNullable(request.getProviderPaymentId());
        if (provided != null) {
            return provided;
        }
        return "DEMO-" + payment.getPaymentProvider() + "-" + payment.getTransactionId();
    }

    private void verifyRazorpaySignature(Payment payment, PaymentConfirmRequestDto request) {
        String orderId = normalizeText(payment.getProviderOrderId());
        String paymentId = normalizeText(request.getProviderPaymentId());
        String signature = normalizeText(request.getProviderSignature());

        if (orderId.isEmpty() || paymentId.isEmpty() || signature.isEmpty()) {
            if (isRazorpaySandboxConfirmation(orderId, paymentId, signature)) {
                return;
            }
            throw new BadRequestException("Missing Razorpay confirmation data");
        }
        if (normalizeText(razorpayKeySecret).isEmpty()) {
            if (isRazorpaySandboxConfirmation(orderId, paymentId, signature)) {
                return;
            }
            throw new BadRequestException("Razorpay secret is not configured");
        }

        String payload = orderId + "|" + paymentId;
        String computed = hmacSha256(payload, razorpayKeySecret);
        if (!computed.equalsIgnoreCase(signature)) {
            if (isRazorpaySandboxConfirmation(orderId, paymentId, signature)) {
                return;
            }
            throw new BadRequestException("Invalid Razorpay payment signature");
        }
    }

    private boolean isRazorpaySandboxConfirmation(String orderId, String paymentId, String signature) {
        boolean demoOrder = orderId.startsWith("rzp_demo_order_");
        boolean demoPayment = paymentId.startsWith("pay_demo_") || paymentId.startsWith("RAZORPAY-");
        boolean demoSignature = signature.isEmpty() || signature.startsWith("demo_signature_");
        boolean sandboxMode = razorpaySandboxMode || demoOrder || !razorpayEnabled || normalizeText(razorpayKeyId).startsWith("rzp_test_");
        return sandboxMode && demoPayment && demoSignature;
    }

    private String buildSuccessMessage(String planCode) {
        if (PaymentConstants.PLAN_VERIFIED.equals(planCode)) {
            return "Payment successful. Your verified badge is now active.";
        }
        return "Payment successful. Your premium subscription is now active.";
    }

    private String paymentReference(Payment payment) {
        return "payment-" + payment.getPaymentId() + "-" + payment.getTransactionId();
    }

    private String humanPlanName(String planCode) {
        return PaymentConstants.PLAN_VERIFIED.equals(planCode) ? "Verified Badge" : "Premium Membership";
    }

    private String normalizeTheme(String theme) {
        return theme == null || theme.isBlank() ? PaymentConstants.THEME_CLASSIC : theme.trim().toUpperCase();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
