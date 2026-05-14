package com.connectsphere.notification.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.entity.NotificationSettings;
import com.connectsphere.notification.producer.NotificationProducer;
import com.connectsphere.notification.service.NotificationService;
import com.connectsphere.notification.sse.SseEmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @Mock private SseEmitterService sseEmitterService;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setRecipientEmail("user@gmail.com");
        testNotification.setSenderEmail("sender@gmail.com");
        testNotification.setType("LIKE");
        testNotification.setMessage("Someone liked your post");
        testNotification.setRead(false);
    }

    @Test
    @DisplayName("POST /notifications — should create notification")
    void createNotification_Success() throws Exception {
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(testNotification);

        mockMvc.perform(post("/notifications")
                        .param("recipientEmail", "user@gmail.com")
                        .param("senderEmail", "sender@gmail.com")
                        .param("type", "LIKE")
                        .param("message", "Someone liked your post"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications — should return user notifications (no pagination)")
    void getMyNotifications_NoPaging() throws Exception {
        when(notificationService.getNotifications("user@gmail.com")).thenReturn(List.of(testNotification));

        mockMvc.perform(get("/notifications")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications — should return paged notifications")
    void getMyNotifications_WithPaging() throws Exception {
        com.connectsphere.notification.dto.NotificationPageResponse page =
                new com.connectsphere.notification.dto.NotificationPageResponse();
        when(notificationService.getNotifications("user@gmail.com", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/notifications")
                        .header("X-User-Email", "user@gmail.com")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/all — should return all notifications")
    void getAll_Success() throws Exception {
        when(notificationService.getAll()).thenReturn(List.of(testNotification));

        mockMvc.perform(get("/notifications/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/unread-count — should return unread count")
    void unreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount("user@gmail.com")).thenReturn(3L);

        mockMvc.perform(get("/notifications/unread-count")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read — should mark notification as read")
    void markRead_Success() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Marked as read"));
    }

    @Test
    @DisplayName("PUT /notifications/{id}/unread — should mark notification as unread")
    void markUnread_Success() throws Exception {
        doNothing().when(notificationService).markAsUnread(1L);

        mockMvc.perform(put("/notifications/1/unread"))
                .andExpect(status().isOk())
                .andExpect(content().string("Marked as unread"));
    }

    @Test
    @DisplayName("PUT /notifications/read-all — should mark all as read")
    void markAllRead_Success() throws Exception {
        doNothing().when(notificationService).markAllAsRead("user@gmail.com");

        mockMvc.perform(put("/notifications/read-all")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("All marked as read"));
    }

    @Test
    @DisplayName("DELETE /notifications/{id} — should delete notification")
    void deleteNotification_Success() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L);

        mockMvc.perform(delete("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));
    }

    @Test
    @DisplayName("GET /notifications/settings — should return notification settings")
    void getSettings_Success() throws Exception {
        NotificationSettings settings = new NotificationSettings();
        when(notificationService.getSettings("user@gmail.com")).thenReturn(settings);

        mockMvc.perform(get("/notifications/settings")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /notifications/mute — should mute notifications")
    void mute_Success() throws Exception {
        doNothing().when(notificationService).mute(eq("user@gmail.com"), isNull());

        mockMvc.perform(put("/notifications/mute")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Muted"));
    }

    @Test
    @DisplayName("PUT /notifications/unmute — should unmute notifications")
    void unmute_Success() throws Exception {
        doNothing().when(notificationService).unmute("user@gmail.com");

        mockMvc.perform(put("/notifications/unmute")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unmuted"));
    }

    @Test
    @DisplayName("GET /notifications/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification Service is running"));
    }

    @Test
    @DisplayName("POST /notifications/event — should publish notification event")
    void publishEvent_Success() throws Exception {
        doNothing().when(notificationProducer).publishNotificationEvent(any());

        mockMvc.perform(post("/notifications/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientEmail\":\"user@gmail.com\",\"type\":\"LIKE\",\"message\":\"Test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/stream — should create SSE emitter")
    void stream_WithUserEmail() throws Exception {
        SseEmitter emitter = new SseEmitter(30000L);
        when(sseEmitterService.create("user@gmail.com")).thenReturn(emitter);

        mockMvc.perform(get("/notifications/stream")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }
}
