package com.connectsphere.notification.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.connectsphere.notification.dto.NotificationEvent;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.entity.NotificationSettings;
import com.connectsphere.notification.producer.NotificationProducer;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.repository.NotificationSettingsRepository;
import com.connectsphere.notification.sse.SseEmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationEventConsumerTest — tests the RabbitMQ consumer that processes notification events.
 * Covers: normal events, LIKE aggregation, HIGH priority email routing, and muted user handling.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSettingsRepository settingsRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationProducer notificationProducer;
    @Mock private SseEmitterService sseEmitterService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    private NotificationEvent testEvent;
    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "notificationQueue", "notification.queue");

        testEvent = new NotificationEvent();
        testEvent.setRecipientEmail("user@gmail.com");
        testEvent.setActorEmail("sender@gmail.com");
        testEvent.setType("FOLLOW");
        testEvent.setMessage("sender@gmail.com started following you");
        testEvent.setPriority("NORMAL");

        savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setRecipientEmail("user@gmail.com");
        savedNotification.setSenderEmail("sender@gmail.com");
        savedNotification.setType("FOLLOW");
        savedNotification.setMessage("sender@gmail.com started following you");
        savedNotification.setRead(false);
    }

    @Test
    @DisplayName("consume — should save FOLLOW notification and push via WebSocket")
    void consume_FollowEvent_SavesAndPushes() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(1L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.empty());

        consumer.consume(testEvent);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    @DisplayName("consume — should aggregate LIKE event when recent one exists")
    void consume_LikeAggregation_UpdatesExisting() {
        testEvent.setType("LIKE");
        testEvent.setTargetId(10L);
        testEvent.setMessage("sender@gmail.com liked your post");

        savedNotification.setType("LIKE");
        savedNotification.setReferenceId(10L);
        savedNotification.setCreatedAt(java.time.LocalDateTime.now().minusMinutes(5));
        savedNotification.setMetadata("alice@gmail.com");

        when(notificationRepository.findTopByRecipientEmailAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
                "user@gmail.com", "LIKE", 10L)).thenReturn(Optional.of(savedNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(2L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.empty());

        consumer.consume(testEvent);

        // The existing notification should have been updated (not a new one saved)
        verify(notificationRepository).save(savedNotification);
        assertTrue(savedNotification.getMessage().contains("liked your post"));
    }

    @Test
    @DisplayName("consume — should publish email for HIGH priority events")
    void consume_HighPriority_PublishesEmail() {
        testEvent.setPriority("HIGH");
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(0L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.empty());

        consumer.consume(testEvent);

        verify(notificationProducer).publishHighPriorityEmail(testEvent);
    }

    @Test
    @DisplayName("consume — should NOT publish email for NORMAL priority events")
    void consume_NormalPriority_SkipsEmail() {
        testEvent.setPriority("NORMAL");
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(0L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.empty());

        consumer.consume(testEvent);

        verify(notificationProducer, never()).publishHighPriorityEmail(any());
    }

    @Test
    @DisplayName("consume — should skip SSE when user has push muted")
    void consume_MutedUser_SkipsSse() {
        NotificationSettings settings = new NotificationSettings();
        settings.setPushEnabled(false);
        settings.setMuted(true);
        settings.setEmailEnabled(true);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(0L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.of(settings));

        consumer.consume(testEvent);

        verify(sseEmitterService, never()).sendEvent(anyString(), any());
    }

    @Test
    @DisplayName("consume — should send SSE when push is allowed")
    void consume_PushEnabled_SendsSse() {
        NotificationSettings settings = new NotificationSettings();
        settings.setPushEnabled(true);
        settings.setMuted(false);
        settings.setEmailEnabled(true);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.countByRecipientEmailAndIsReadFalse("user@gmail.com")).thenReturn(0L);
        when(settingsRepository.findByRecipientEmail("user@gmail.com")).thenReturn(Optional.of(settings));

        consumer.consume(testEvent);

        verify(sseEmitterService, atLeastOnce()).sendEvent(anyString(), any());
    }
}
