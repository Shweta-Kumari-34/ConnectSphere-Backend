package com.connectsphere.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import com.connectsphere.payment.repository.SubscriptionRepository;
import com.connectsphere.payment.exception.BadRequestException;
import com.connectsphere.payment.exception.ResourceNotFoundException;
import com.connectsphere.payment.service.EmailService;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.connectsphere.payment.dto.PaymentConfirmRequestDto;
import com.connectsphere.payment.entity.Subscription;
import com.connectsphere.payment.entity.Subscription.SubscriptionStatus;
import com.connectsphere.payment.util.PaymentConstants;

import com.connectsphere.payment.dto.*;
import com.connectsphere.payment.entity.Payment;
import com.connectsphere.payment.entity.Payment.PaymentStatus;
import com.connectsphere.payment.repository.PaymentRepository;
import com.connectsphere.payment.service.impl.PaymentServiceImpl;

/*
 * PaymentServiceImplTest
 * ----------------------
 * Unit tests for PaymentServiceImpl using JUnit 5 + Mockito.
 *
 * Tests:
 *   1. processPayment() — should create payment with SUCCESS status
 *   2. getPaymentsByUser() — should return user's payments
 *   3. getAllPayments() — should return all payments
 *   4. getPaymentById() — success
 *   5. getPaymentById() — not found throws exception
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    private PaymentServiceImpl paymentService;

    @Mock
    private RestTemplate restTemplate;

    private PaymentRequestDto paymentRequest;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        // Manually instantiate to ensure final fields are set correctly
        paymentService = new PaymentServiceImpl(paymentRepository, subscriptionRepository, restTemplateBuilder, emailService);
        
        paymentRequest = new PaymentRequestDto();
        paymentRequest.setAmount(new BigDecimal("999.00"));
        paymentRequest.setPlanCode("PREMIUM_MEMBERSHIP");
        paymentRequest.setDescription("Premium subscription");
        paymentRequest.setPaymentMethod("UPI");
        paymentRequest.setPaymentProvider("UPI_QR");

        testPayment = new Payment();
        testPayment.setPaymentId(1L);
        testPayment.setUserEmail("user@gmail.com");
        testPayment.setAmount(new BigDecimal("999.00"));
        testPayment.setPlanCode("PREMIUM_MEMBERSHIP");
        testPayment.setDescription("Premium subscription");
        testPayment.setPaymentMethod("UPI");
        testPayment.setPaymentProvider("UPI_QR");
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setTransactionId("TXN-ABCD1234");
        testPayment.setExpiresAt(LocalDateTime.now().plusHours(1));

        ReflectionTestUtils.setField(paymentService, "demoUpiVirtualAddress", "demo@upi");
        ReflectionTestUtils.setField(paymentService, "demoUpiMerchantName", "Demo Merchant");
        ReflectionTestUtils.setField(paymentService, "authServiceUrl", "http://auth");
        ReflectionTestUtils.setField(paymentService, "internalSecret", "secret");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "secret");
    }

    @Test
    @DisplayName("CreatePaymentIntent - should create payment with PENDING status")
    void createPaymentIntent_Success() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setPaymentId(1L);
            return p;
        });

        PaymentResponseDto response = paymentService.createPaymentIntent("user@gmail.com", paymentRequest);

        assertNotNull(response);
        assertEquals("Demo UPI QR ready. No real bank account is required. Scan or review the payload, then click Confirm Payment.", response.getMessage());
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("999.00"), response.getAmount());
        assertNotNull(response.getTransactionId());
        assertTrue(response.getTransactionId().startsWith("TXN-"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("GetPaymentsByUser - should return user's payments")
    void getPaymentsByUser_Success() {
        when(paymentRepository.findByUserEmailOrderByCreatedAtDesc("user@gmail.com")).thenReturn(List.of(testPayment));

        List<Payment> result = paymentService.getPaymentsByUser("user@gmail.com");

        assertEquals(1, result.size());
        assertEquals("user@gmail.com", result.get(0).getUserEmail());
    }

    @Test
    @DisplayName("GetAllPayments - should return all payments")
    void getAllPayments_Success() {
        when(paymentRepository.findAll()).thenReturn(Arrays.asList(testPayment));

        List<Payment> result = paymentService.getAllPayments();

        assertFalse(result.isEmpty());
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName("GetPaymentById - should return payment when found")
    void getPaymentById_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPaymentId());
    }

    @Test
    @DisplayName("GetPaymentById - should throw when not found")
    void getPaymentById_NotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
 
        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(99L));
    }
    @Test
    @DisplayName("ConfirmPayment - should activate subscription on success")
    void confirmPayment_Success() {
        PaymentConfirmRequestDto confirmRequest = new PaymentConfirmRequestDto();
        confirmRequest.setPaymentId(1L);
        confirmRequest.setSuccess(true);
        confirmRequest.setProviderPaymentId("PROV-123");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        
        // Mock profile lookup from Auth Service (getForObject)
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", 12345L);
        profile.put("email", "user@gmail.com");
        
        lenient().when(restTemplate.getForObject(anyString(), eq(Map.class), anyString()))
                .thenReturn(profile);
        
        // Mock subscription sync to Auth Service (POST)
        lenient().doReturn(ResponseEntity.ok(new HashMap<>()))
            .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(Class.class), (Object[]) any());

        when(subscriptionRepository.findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponseDto response = paymentService.confirmPayment("user@gmail.com", confirmRequest);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getReceiptNumber());
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(emailService, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("ConfirmPayment - should throw when payment belongs to other user")
    void confirmPayment_UserMismatch() {
        PaymentConfirmRequestDto confirmRequest = new PaymentConfirmRequestDto();
        confirmRequest.setPaymentId(1L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        assertThrows(BadRequestException.class, 
            () -> paymentService.confirmPayment("wrong@gmail.com", confirmRequest));
    }

    @Test
    @DisplayName("CancelSubscription - should mark as cancelled")
    void cancelSubscription_Success() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setUserEmail("user@gmail.com");
        sub.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        Map<String, Object> result = paymentService.cancelSubscription("user@gmail.com", 10L);

        assertEquals("CANCELLED", result.get("status"));
        assertEquals("Subscription cancelled", result.get("message"));
        verify(subscriptionRepository).save(sub);
    }

    @Test
    @DisplayName("HandleWebhook - should confirm payment on success status")
    void handleWebhook_Success() {
        PaymentWebhookRequestDto webhookRequest = new PaymentWebhookRequestDto();
        webhookRequest.setPaymentId(1L);
        webhookRequest.setStatus("SUCCESS");
        webhookRequest.setEventType("PAYMENT_CONFIRMED");
        webhookRequest.setTimestamp(System.currentTimeMillis());
        webhookRequest.setProviderPaymentId("WEB-123");

        // Mock signature verification
        String payload = webhookRequest.getPaymentId() + ":" + webhookRequest.getEventType() 
                         + ":" + webhookRequest.getStatus() + ":" + webhookRequest.getTimestamp();
        String signature = hmacSha256(payload, "secret");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        
        // Mock profile and activation calls
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", 12345L);
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(any(), any(), (Object[]) any())).thenReturn(profile);
        lenient().doReturn(ResponseEntity.ok(new HashMap<>())).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(Class.class), (Object[]) any());

        Map<String, Object> result = paymentService.handleWebhook(webhookRequest, signature);

        assertEquals("OK", result.get("status"));
        assertEquals("Payment confirmed from webhook", result.get("message"));
    }

    @Test
    @DisplayName("HandleWebhook - should throw on invalid signature")
    void handleWebhook_InvalidSignature() {
        PaymentWebhookRequestDto webhookRequest = new PaymentWebhookRequestDto();
        webhookRequest.setPaymentId(1L);

        assertThrows(BadRequestException.class, 
            () -> paymentService.handleWebhook(webhookRequest, "wrong-sig"));
    }

    private String hmacSha256(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @DisplayName("RenewSubscription - should extend expiry and notify auth")
    void renewSubscription_Success() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setUserEmail("user@gmail.com");
        sub.setPlanCode("PREMIUM_MEMBERSHIP");
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setExpiresAt(LocalDateTime.now().plusDays(1));

        SubscriptionActionRequestDto request = new SubscriptionActionRequestDto();
        request.setAutoRenew(true);

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setPaymentId(1L);
            return p;
        });
        
        // Mock profile lookup for activateSubscription
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", 12345L);
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(any(), any(), (Object[]) any())).thenReturn(profile);
        lenient().doReturn(ResponseEntity.ok(new HashMap<>())).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(Class.class), (Object[]) any());

        Map<String, Object> result = paymentService.renewSubscription("user@gmail.com", 10L, request);

        assertEquals("ACTIVE", result.get("status"));
    }

    @Test
    @DisplayName("SetAutoRenew - should update auto-renew flag")
    void setAutoRenew_Success() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setUserEmail("user@gmail.com");
        sub.setAutoRenew(false);
        sub.setStatus(SubscriptionStatus.ACTIVE);

        SubscriptionActionRequestDto request = new SubscriptionActionRequestDto();
        request.setAutoRenew(true);

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);
        lenient().doReturn(ResponseEntity.ok(new HashMap<>())).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(Class.class), (Object[]) any());

        Map<String, Object> result = paymentService.setAutoRenew("user@gmail.com", 10L, request);

        assertEquals("ACTIVE", result.get("status"));
        assertTrue(sub.isAutoRenew());
    }

    @Test
    @DisplayName("UpgradeSubscription - should change plan and notify auth")
    void upgradeSubscription_Success() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setUserEmail("user@gmail.com");
        sub.setPlanCode("VERIFIED_BADGE");
        sub.setStatus(SubscriptionStatus.ACTIVE);

        SubscriptionActionRequestDto request = new SubscriptionActionRequestDto();
        request.setTargetPlanCode("PREMIUM_MEMBERSHIP");

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setPaymentId(1L);
            return p;
        });
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(testPayment));
        
        // Mock profile lookup
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", 12345L);
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(any(), any(), (Object[]) any())).thenReturn(profile);

        // Mock the find call for the upgraded subscription
        Subscription upgradedSub = new Subscription();
        upgradedSub.setPlanCode("PREMIUM_MEMBERSHIP");
        upgradedSub.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findFirstByUserEmailAndPlanCodeOrderByStartedAtDesc(anyString(), anyString()))
                .thenReturn(Optional.of(upgradedSub));

        lenient().doReturn(ResponseEntity.ok(new HashMap<>())).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(Class.class), (Object[]) any());

        Map<String, Object> result = paymentService.upgradeSubscription("user@gmail.com", 10L, request);

        assertEquals("ACTIVE", result.get("status"));
    }

    @Test
    @DisplayName("DownloadReceiptPdf - should generate bytes")
    void downloadReceiptPdf_Success() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setPaymentId(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(anyString(), eq(Map.class), (Object) any())).thenReturn(profile);

        byte[] pdf = paymentService.downloadReceiptPdf("user@gmail.com", 1L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("GetDueRenewalReminders - should return list of maps")
    void getDueRenewalReminders_Success() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setUserEmail("user@gmail.com");
        sub.setPlanCode("PREMIUM");
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setExpiresAt(LocalDateTime.now().plusDays(2));
        sub.setRenewalReminderSent(false);

        when(subscriptionRepository.findByStatusAndRenewalReminderSentFalse(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        List<Map<String, Object>> reminders = paymentService.getDueRenewalReminders();

        assertNotNull(reminders);
        assertFalse(reminders.isEmpty());
        assertEquals("user@gmail.com", reminders.get(0).get("userEmail"));
    }

    @Test
    @DisplayName("GetReceipt - should return structured map")
    void getReceipt_Success() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setReceiptNumber("RCP-123");
        testPayment.setTransactionId("TXN-123");
        testPayment.setConfirmedAt(LocalDateTime.now());
        testPayment.setPaymentMethod("UPI");
        testPayment.setPaymentProvider("RAZORPAY");
        testPayment.setAmount(new BigDecimal("999.00"));
        testPayment.setCurrency("INR");
        testPayment.setPlanCode("PREMIUM_MEMBERSHIP");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(any(), any(), (Object[]) any())).thenReturn(profile);

        Map<String, Object> receipt = paymentService.getReceipt("user@gmail.com", 1L);

        assertNotNull(receipt);
        assertEquals("RCP-123", receipt.get("receiptNumber"));
    }

    @Test
    @DisplayName("HandleWebhook - should return IGNORED if payment not found")
    void handleWebhook_NotFound() {
        PaymentWebhookRequestDto request = new PaymentWebhookRequestDto();
        request.setPaymentId(999L);
        request.setEventType("payment.captured");
        request.setStatus("SUCCESS");
        request.setTimestamp(System.currentTimeMillis());

        // Compute valid signature for the request
        String payload = "999:payment.captured:SUCCESS:" + request.getTimestamp();
        String signature = hmacSha256(payload, "secret");

        when(paymentRepository.findById(999L)).thenReturn(Optional.ofNullable(null));

        Map<String, Object> result = paymentService.handleWebhook(request, signature);

        assertEquals("IGNORED", result.get("status"));
    }


    @Test
    @DisplayName("CreatePaymentIntent - Razorpay - should call Razorpay API")
    void createPaymentIntent_Razorpay() {
        ReflectionTestUtils.setField(paymentService, "razorpayEnabled", true);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "rzp_secret");

        PaymentRequestDto request = new PaymentRequestDto();
        request.setAmount(new BigDecimal("500.00"));
        request.setPlanCode("PREMIUM_MEMBERSHIP");
        request.setPaymentProvider("RAZORPAY");
        request.setPaymentMethod("CARD");

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setPaymentId(1L);
            return p;
        });

        Map<String, Object> rzpResponse = new HashMap<>();
        rzpResponse.put("id", "order_123");
        lenient().when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(rzpResponse));

        PaymentResponseDto response = paymentService.createPaymentIntent("user@gmail.com", request);

        assertNotNull(response);
        assertEquals("order_123", response.getRazorpayOrderId());
    }

    @Test
    @DisplayName("ConfirmPayment - Razorpay - should verify signature")
    void confirmPayment_Razorpay() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "rzp_secret");
        
        testPayment.setPaymentProvider("RAZORPAY");
        testPayment.setProviderOrderId("order_123");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentConfirmRequestDto confirm = new PaymentConfirmRequestDto();
        confirm.setPaymentId(1L);
        confirm.setProviderPaymentId("pay_123");
        confirm.setSuccess(true);
        
        // Compute valid Razorpay signature: order_id + "|" + payment_id
        String payload = "order_123|pay_123";
        String signature = hmacSha256(payload, "rzp_secret");
        confirm.setProviderSignature(signature);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", 12345L);
        profile.put("fullName", "John Doe");
        lenient().when(restTemplate.getForObject(any(), any(), (Object[]) any())).thenReturn(profile);

        paymentService.confirmPayment("user@gmail.com", confirm);

        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
    }
}
