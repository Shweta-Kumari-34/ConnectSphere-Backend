package com.connectsphere.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.repository.NotificationSettingsRepository;
import com.connectsphere.notification.exception.ResourceNotFoundException;
import com.connectsphere.notification.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingsRepository settingsRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setRecipientEmail("user@gmail.com");
        testNotification.setSenderEmail("sender@gmail.com");
        testNotification.setType("LIKE");
        testNotification.setMessage("Someone liked your post");
        testNotification.setReferenceId(10L);
        testNotification.setRead(false);
    }

    @Test
    @DisplayName("CreateNotification - should save and return notification")
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createNotification(
                "user@gmail.com", "sender@gmail.com", "LIKE", "Someone liked your post", 10L);

        assertNotNull(result);
        assertEquals("LIKE", result.getType());
        assertEquals("user@gmail.com", result.getRecipientEmail());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("GetNotifications - should return user notifications")
    void getNotifications_Success() {
        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc("user@gmail.com"))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getNotifications("user@gmail.com");

        assertEquals(1, result.size());
        assertEquals("user@gmail.com", result.get(0).getRecipientEmail());
    }

    @Test
    @DisplayName("GetUnreadCount - should return unread count")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount("user@gmail.com"));
    }

    @Test
    @DisplayName("MarkAsRead - should set isRead to true")
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(1L);

        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("MarkAsRead - should throw when not found")
    void markAsRead_NotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L));
    }

    @Test
    @DisplayName("MarkAllAsRead - should mark all unread as read")
    void markAllAsRead_Success() {
        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setRecipientEmail("user@gmail.com");
        n2.setRead(false);

        when(notificationRepository.findByRecipientEmailAndIsReadFalse("user@gmail.com"))
                .thenReturn(List.of(testNotification, n2));

        notificationService.markAllAsRead("user@gmail.com");

        assertTrue(testNotification.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("DeleteNotification - should delete by id")
    void deleteNotification_Success() {
        notificationService.deleteNotification(1L);

        verify(notificationRepository).deleteById(1L);
    }
}
