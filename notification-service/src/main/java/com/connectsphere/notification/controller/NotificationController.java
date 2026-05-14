package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.BroadcastRequest;
import com.connectsphere.notification.dto.NotificationPageResponse;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.connectsphere.notification.entity.NotificationSettings;
import com.connectsphere.notification.sse.SseEmitterService;
import com.connectsphere.notification.producer.NotificationProducer;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * NotificationController
 * ----------------------
 * Handles notification CRUD, stream delivery (SSE), user settings, and event ingestion.
 * Multiple clients use this: frontend, internal services, and async consumers.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    private final NotificationProducer notificationProducer;

    public NotificationController(NotificationService notificationService, SseEmitterService sseEmitterService, NotificationProducer notificationProducer) {
        this.notificationService = notificationService;
        this.sseEmitterService = sseEmitterService;
        this.notificationProducer = notificationProducer;
    }

    @PostMapping
    public ResponseEntity<Notification> create(@RequestParam String recipientEmail, @RequestParam String senderEmail,
                                                @RequestParam String type, @RequestParam String message,
                                                @RequestParam(required = false) Long referenceId,
                                                @RequestParam(required = false) String actionUrl,
                                                @RequestParam(required = false) String referenceType) {
        // Direct create endpoint used by internal calls and admin actions.
        return ResponseEntity.ok(notificationService.createNotification(
                recipientEmail,
                senderEmail,
                type,
                message,
                referenceId,
                actionUrl,
                referenceType
        ));
    }

    @PostMapping("/event")
    public ResponseEntity<String> publishEvent(@RequestBody Map<String, Object> payload) {
        // Accepts flexible JSON payload and maps to NotificationEvent for producer pipeline.
        log.info("publishEvent called with payload={}", payload);
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(payload);
            com.connectsphere.notification.dto.NotificationEvent event = mapper.readValue(jsonString, com.connectsphere.notification.dto.NotificationEvent.class);
            log.info("Parsed JSON to NotificationEvent recipient={} type={}", event.getRecipientEmail(), event.getType());
            notificationProducer.publishNotificationEvent(event);
            return ResponseEntity.ok("Event published");
        } catch (Exception ex) {
            log.error("Failed to publish notification event: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Failed to publish event: " + ex.getMessage());
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> sendBulk(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) request.get("recipientEmails");
        String type = (String) request.get("type");
        String message = (String) request.get("message");
        notificationService.sendBulkNotification(recipients, type, message);
        return ResponseEntity.ok("Bulk notifications sent to " + recipients.size() + " users");
    }

    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@RequestBody BroadcastRequest request) {
        List<String> recipients = request.getRecipientEmails();
        if (recipients == null || recipients.isEmpty()) {
            return ResponseEntity.badRequest().body("recipientEmails is required");
        }
        notificationService.sendBulkNotification(recipients, request.getType(), request.getMessage());
        return ResponseEntity.ok("Broadcast notifications sent to " + recipients.size() + " users");
    }

    @GetMapping
    public ResponseEntity<?> getMyNotifications(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null || size != null) {
            return ResponseEntity.ok(notificationService.getNotifications(userEmail, page == null ? 0 : page, size == null ? 20 : size));
        }
        return ResponseEntity.ok(notificationService.getNotifications(userEmail));
    }

    @GetMapping("/user/{recipientEmail}")
    public ResponseEntity<NotificationPageResponse> getNotificationsByUser(
            @PathVariable String recipientEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(recipientEmail, page, size));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userEmail));
    }

    @GetMapping("/unread-count/{recipientEmail}")
    public ResponseEntity<Long> unreadCountByUser(@PathVariable String recipientEmail) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientEmail));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestHeader(value = "X-User-Email", required = false) String userEmail,
                             @RequestParam(value = "email", required = false) String emailParam,
                             @RequestParam(value = "userId", required = false) String userIdParam) {
        // Resolve best available client key for SSE channel binding.
        String key = null;
        if (userIdParam != null && !userIdParam.isBlank()) {
            key = userIdParam;
        } else if (userEmail != null && !userEmail.isBlank()) {
            key = userEmail;
        } else if (emailParam != null && !emailParam.isBlank()) {
            key = emailParam;
        }
        if (key == null) key = "anonymous";
        return sseEmitterService.create(key);
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettings> getSettings(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(notificationService.getSettings(userEmail));
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettings> updateSettings(@RequestHeader("X-User-Email") String userEmail,
                                                               @RequestBody NotificationSettings settings) {
        return ResponseEntity.ok(notificationService.updateSettings(userEmail, settings));
    }

    @PutMapping("/mute")
    public ResponseEntity<String> mute(@RequestHeader("X-User-Email") String userEmail,
                                       @RequestParam(value = "until", required = false) String untilIso) {
        LocalDateTime until = null;
        if (untilIso != null) {
            until = LocalDateTime.parse(untilIso);
        }
        notificationService.mute(userEmail, until);
        return ResponseEntity.ok("Muted");
    }

    @PutMapping("/unmute")
    public ResponseEntity<String> unmute(@RequestHeader("X-User-Email") String userEmail) {
        notificationService.unmute(userEmail);
        return ResponseEntity.ok("Unmuted");
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id); return ResponseEntity.ok("Marked as read");
    }

    @PutMapping("/{id}/unread")
    public ResponseEntity<String> markUnread(@PathVariable Long id) {
        notificationService.markAsUnread(id);
        return ResponseEntity.ok("Marked as unread");
    }

    @PutMapping("/read-all")
    public ResponseEntity<String> markAllRead(@RequestHeader("X-User-Email") String userEmail) {
        // Bulk operation for inbox cleanup.
        notificationService.markAllAsRead(userEmail); return ResponseEntity.ok("All marked as read");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id); return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/test")
    public String test() {
        log.info("TEST ENDPOINT CALLED - verify logging works");
        return "Notification Service is running";
    }
}
