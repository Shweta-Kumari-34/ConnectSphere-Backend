package com.connectsphere.payment.controller;

import com.connectsphere.payment.dto.*;
import com.connectsphere.payment.service.PaymentService;
import com.connectsphere.payment.entity.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequestDto paymentRequest;
    private PaymentResponseDto paymentResponse;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequestDto();
        paymentRequest.setAmount(new BigDecimal("999.00"));
        paymentRequest.setPlanCode("PREMIUM");
        paymentRequest.setPaymentProvider("UPI_QR");
        paymentRequest.setPaymentMethod("UPI");

        paymentResponse = new PaymentResponseDto();
        paymentResponse.setPaymentId(1L);
        paymentResponse.setStatus("PENDING");
        paymentResponse.setRazorpayOrderId("ORD-123");
    }

    @Test
    @DisplayName("POST /payments/intent - should return pending payment")
    void initiatePayment_Success() throws Exception {
        when(paymentService.createPaymentIntent(anyString(), any(PaymentRequestDto.class)))
                .thenReturn(paymentResponse);

        mockMvc.perform(post("/payments/intent")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test
    @DisplayName("POST /payments/process - should confirm legacy payment")
    void processLegacyPayment_Success() throws Exception {
        when(paymentService.createPaymentIntent(anyString(), any(PaymentRequestDto.class)))
                .thenReturn(paymentResponse);
        when(paymentService.confirmPayment(anyString(), any(PaymentConfirmRequestDto.class)))
                .thenReturn(paymentResponse);

        mockMvc.perform(post("/payments/process")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /payments/confirm - should return success")
    void confirmPayment_Success() throws Exception {
        PaymentConfirmRequestDto confirmRequest = new PaymentConfirmRequestDto();
        confirmRequest.setPaymentId(1L);
        confirmRequest.setSuccess(true);

        PaymentResponseDto successResponse = new PaymentResponseDto();
        successResponse.setStatus("SUCCESS");

        when(paymentService.confirmPayment(anyString(), any(PaymentConfirmRequestDto.class)))
                .thenReturn(successResponse);

        mockMvc.perform(post("/payments/confirm")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/my - should return user payments")
    void getMyPayments_Success() throws Exception {
        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment());
        when(paymentService.getPaymentsByUser("user@gmail.com")).thenReturn(payments);

        mockMvc.perform(get("/payments/my")
                .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /payments/all - should return all payments")
    void getAllPayments_Success() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/payments/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/{id} - should return payment")
    void getPaymentById_Success() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentId(1L);
        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/subscriptions/my - should return subscriptions")
    void getMySubscriptions_Success() throws Exception {
        when(paymentService.getMySubscriptions(anyString())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/payments/subscriptions/my")
                .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/subscriptions/current - should return current subscription")
    void getCurrentSubscription_Success() throws Exception {
        when(paymentService.getCurrentSubscription(anyString(), anyString())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/payments/subscriptions/current")
                .header("X-User-Email", "user@gmail.com")
                .param("planCode", "PREMIUM"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /payments/subscriptions/{id}/renew - should renew")
    void renewSubscription_Success() throws Exception {
        when(paymentService.renewSubscription(anyString(), anyLong(), any())).thenReturn(new HashMap<>());
        mockMvc.perform(post("/payments/subscriptions/10/renew")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /payments/subscriptions/{id}/cancel - should cancel")
    void cancelSubscription_Success() throws Exception {
        when(paymentService.cancelSubscription(anyString(), anyLong())).thenReturn(new HashMap<>());
        mockMvc.perform(put("/payments/subscriptions/10/cancel")
                .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /payments/subscriptions/{id}/auto-renew - should set auto-renew")
    void setAutoRenew_Success() throws Exception {
        SubscriptionActionRequestDto request = new SubscriptionActionRequestDto();
        when(paymentService.setAutoRenew(anyString(), anyLong(), any())).thenReturn(new HashMap<>());
        mockMvc.perform(put("/payments/subscriptions/10/auto-renew")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /payments/subscriptions/{id}/upgrade - should upgrade")
    void upgradeSubscription_Success() throws Exception {
        SubscriptionActionRequestDto request = new SubscriptionActionRequestDto();
        when(paymentService.upgradeSubscription(anyString(), anyLong(), any())).thenReturn(new HashMap<>());
        mockMvc.perform(post("/payments/subscriptions/10/upgrade")
                .header("X-User-Email", "user@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/receipts/{paymentId} - should return receipt")
    void getReceipt_Success() throws Exception {
        when(paymentService.getReceipt(anyString(), anyLong())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/payments/receipts/1")
                .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/receipts/{paymentId}/download - should download PDF")
    void downloadReceiptPdf_Success() throws Exception {
        byte[] content = "dummy-pdf".getBytes();
        when(paymentService.downloadReceiptPdf(anyString(), anyLong())).thenReturn(content);
        mockMvc.perform(get("/payments/receipts/1/download")
                .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @DisplayName("GET /payments/renewal-reminders/due - should return due reminders")
    void getDueRenewalReminders_Success() throws Exception {
        when(paymentService.getDueRenewalReminders()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/payments/renewal-reminders/due"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /payments/webhook - should handle webhook")
    void handleWebhook_Success() throws Exception {
        PaymentWebhookRequestDto webhookRequest = new PaymentWebhookRequestDto();
        webhookRequest.setPaymentId(1L);
        when(paymentService.handleWebhook(any(), anyString())).thenReturn(new HashMap<>());

        mockMvc.perform(post("/payments/webhook")
                .header("X-Webhook-Signature", "sig-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/test - should return heartbeat")
    void testHeartbeat() throws Exception {
        mockMvc.perform(get("/payments/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment Service is running"));
    }
}
