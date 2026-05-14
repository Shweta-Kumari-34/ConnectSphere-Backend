package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Report} entities.
 * <p>
 * Provides administrative queries to fetch reports by status, target type, or reporter.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ReportRepository {
 *         +findByStatus(String)
 *         +findByTargetTypeAndTargetId(String, Long)
 *     }
 *     ReportService --> ReportRepository : Moderation Lookup
 * </pre>
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(String status);

    List<Report> findByReporterEmail(String reporterEmail);

    List<Report> findByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByStatus(String status);

    List<Report> findAllByOrderByCreatedAtDesc();
}
