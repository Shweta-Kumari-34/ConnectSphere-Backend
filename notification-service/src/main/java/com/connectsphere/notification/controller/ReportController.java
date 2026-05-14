package com.connectsphere.notification.controller;

import com.connectsphere.notification.entity.Report;
import com.connectsphere.notification.repository.ReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * ReportController — REST API for content reports.
 * Handles user-submitted reports and admin moderation actions.
 *
 * Endpoints:
 *   POST   /reports                → Submit a new report (user)
 *   GET    /reports                → Get all reports (admin)
 *   GET    /reports/pending        → Get pending reports (admin)
 *   GET    /reports/stats          → Report statistics (admin)
 *   PUT    /reports/{id}/resolve   → Resolve a report (admin)
 *   PUT    /reports/{id}/dismiss   → Dismiss a report (admin)
 *   DELETE /reports/{id}           → Delete a report (admin)
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // POST /reports — Submit a report (user)
    @PostMapping
    public ResponseEntity<Report> submitReport(@RequestHeader("X-User-Email") String userEmail,
                                                @RequestBody Map<String, Object> body) {
        Report report = new Report();
        report.setReporterEmail(userEmail);
        report.setTargetType((String) body.get("targetType"));
        report.setTargetId(Long.valueOf(body.get("targetId").toString()));
        report.setReason((String) body.get("reason"));
        return ResponseEntity.ok(reportRepository.save(report));
    }

    // GET /reports — Get all reports sorted by newest (admin)
    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAllByOrderByCreatedAtDesc());
    }

    // GET /reports/pending — Get only pending reports (admin)
    @GetMapping("/pending")
    public ResponseEntity<List<Report>> getPendingReports() {
        return ResponseEntity.ok(reportRepository.findByStatus("PENDING"));
    }

    // GET /reports/stats — Report statistics (admin dashboard)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getReportStats() {
        long total = reportRepository.count();
        long pending = reportRepository.countByStatus("PENDING");
        long resolved = reportRepository.countByStatus("RESOLVED");
        long dismissed = reportRepository.countByStatus("DISMISSED");
        return ResponseEntity.ok(Map.of(
                "total", total, "pending", pending,
                "resolved", resolved, "dismissed", dismissed));
    }

    // PUT /reports/{id}/resolve — Resolve report (admin)
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Report> resolveReport(@PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, String> body) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("RESOLVED");
        report.setResolvedAt(LocalDateTime.now());
        if (body != null && body.get("adminNote") != null) {
            report.setAdminNote(body.get("adminNote"));
        }
        return ResponseEntity.ok(reportRepository.save(report));
    }

    // PUT /reports/{id}/dismiss — Dismiss report (admin)
    @PutMapping("/{id}/dismiss")
    public ResponseEntity<Report> dismissReport(@PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, String> body) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("DISMISSED");
        report.setResolvedAt(LocalDateTime.now());
        if (body != null && body.get("adminNote") != null) {
            report.setAdminNote(body.get("adminNote"));
        }
        return ResponseEntity.ok(reportRepository.save(report));
    }

    // DELETE /reports/{id} — Delete report permanently (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReport(@PathVariable Long id) {
        reportRepository.deleteById(id);
        return ResponseEntity.ok("Report deleted");
    }

    @GetMapping("/test")
    public String test() { return "Report Service is running"; }
}
