package com.connectsphere.payment.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for dispatching payment receipts and renewal reminders via email.
 * <p>
 * Includes a fallback mechanism to write HTML emails to a local directory 
 * if SMTP is not configured.
 * </p>
 *
 * <h3>Email Workflow</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[PaymentService] -->|Generate Receipt| B(EmailService);
 *     B -->|SMTP Configured| C[Send Mail];
 *     B -->|SMTP Missing| D[Write Local Demo HTML];
 * </pre>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${connectsphere.demo-email.enabled:true}")
    private boolean demoEmailEnabled;

    @Value("${connectsphere.demo-email.outbox-dir:demo-outbox}")
    private String demoOutboxDir;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public boolean sendEmail(String to, String subject, String textBody) {
        if (!isConfigured()) {
            return writeDemoEmail(to, subject, textBody, "SMTP is not configured");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtml(subject, textBody), true);
            javaMailSender.send(message);
            return true;
        } catch (MailException ex) {
            return writeDemoEmail(to, subject, textBody, "SMTP send failed: " + ex.getMessage());
        } catch (Exception ex) {
            return writeDemoEmail(to, subject, textBody, "Email send failed: " + ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return fromEmail != null && !fromEmail.isBlank()
                && smtpPassword != null && !smtpPassword.isBlank();
    }

    private String buildHtml(String title, String body) {
        String safeBody = (body == null ? "" : body).replace("\n", "<br/>");
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#f5f6fa;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;background:#f5f6fa;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 6px 30px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="padding:24px 32px;background:linear-gradient(135deg,#0f172a,#2563eb);color:#ffffff;">
                        <h1 style="margin:0;font-size:24px;">ConnectSphere</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;color:#334155;font-size:15px;line-height:1.7;">
                        <h2 style="margin:0 0 16px;color:#0f172a;font-size:22px;">%s</h2>
                        <p style="margin:0;">%s</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, title, safeBody);
    }

    private boolean writeDemoEmail(String to, String subject, String textBody, String reason) {
        if (!demoEmailEnabled) {
            log.warn("{}; demo email fallback disabled for {} with subject '{}'", reason, to, subject);
            return false;
        }

        try {
            Path outboxPath = Paths.get(demoOutboxDir).toAbsolutePath().normalize();
            Files.createDirectories(outboxPath);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeRecipient = to.replaceAll("[^A-Za-z0-9@._-]", "_");
            Path emailFile = outboxPath.resolve(timestamp + "-" + safeRecipient + ".html");

            String html = buildHtml(subject, textBody)
                    + "\n<!-- Demo email fallback reason: " + reason + " -->\n";
            Files.writeString(emailFile, html, StandardCharsets.UTF_8);
            log.info("Demo receipt email written to {}", emailFile);
            return true;
        } catch (Exception ex) {
            log.warn("{}; unable to write demo email for {} with subject '{}': {}", reason, to, subject, ex.getMessage());
            return false;
        }
    }
}
