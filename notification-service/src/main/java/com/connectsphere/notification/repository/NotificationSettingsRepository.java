package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link NotificationSettings} entities.
 * <p>
 * Provides lookups based on the recipient's email to determine if a notification
 * should be dispatched or suppressed.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class NotificationSettingsRepository {
 *         +findByRecipientEmail(String)
 *     }
 *     NotificationService --> NotificationSettingsRepository : Checks Preferences
 * </pre>
 */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    Optional<NotificationSettings> findByRecipientEmail(String recipientEmail);
}
