package com.connectsphere.auth.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h1>EmailService</h1>
 * <p>A centralized communication engine responsible for dispatching system-generated emails, 
 * including security OTPs and transaction notifications.</p>
 * 
 * <h2>Email Delivery Flow:</h2>
 * <pre>
 * graph TD
 *     Trigger[Action Triggered] --> Template{Select Template}
 *     Template -- SIGNUP/LOGIN --> OTP[Build OTP HTML]
 *     Template -- ALERT --> Notif[Build Notification HTML]
 *     OTP --> Config{SMTP Configured?}
 *     Notif --> Config
 *     Config -- Yes --> Send[Dispatch via JavaMailSender]
 *     Config -- No --> Log[Log to Console/Debug]
 *     Send --> Success[Email Delivered]
 *     Send -- Error --> Retry[Log Error & Fail Silently]
 * </pre>
 * 
 * <h2>Capabilities:</h2>
 * <ul>
 *     <li><b>Asynchronous Delivery:</b> Uses {@code @Async} for non-blocking notification emails.</li>
 *     <li><b>Rich HTML Templates:</b> Premium, responsive HTML designs for all outgoing communications.</li>
 *     <li><b>MIME Support:</b> Full support for international characters and complex layouts.</li>
 *     <li><b>Self-Healing Configuration:</b> Validates SMTP credentials on startup and provides meaningful warnings.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @PostConstruct
    void validateMailConfiguration() {
        boolean usernameLoaded = fromEmail != null && !fromEmail.trim().isEmpty();
        String passwordFromEnv = System.getenv("GMAIL_APP_PASSWORD");
        boolean gmailPasswordEnvExists = passwordFromEnv != null && !passwordFromEnv.trim().isEmpty();
        boolean smtpPasswordLoaded = smtpPassword != null && !smtpPassword.trim().isEmpty();

        log.info("Mail startup validation -> usernameLoaded: {}, gmailAppPasswordEnvExists: {}, smtpPasswordLoaded: {}",
                usernameLoaded, gmailPasswordEnvExists, smtpPasswordLoaded);

        if (!usernameLoaded || !smtpPasswordLoaded) {
            log.warn("Mail SMTP appears incomplete. Set GMAIL_USERNAME and GMAIL_APP_PASSWORD before sending OTP emails.");
        }
    }

    public boolean sendOtpEmail(String to, String otp, String purpose) {
        String subject;
        String htmlBody;

        switch (purpose) {
            case "SIGNUP" -> {
                subject = "ConnectSphere - Verify Your Email Address";
                htmlBody = buildOtpHtml(
                        "Verify Your Email",
                        "Welcome to ConnectSphere! Use the code below to verify your email address and activate your account.",
                        otp,
                        "This code expires in <b>10 minutes</b>.",
                        "If you did not create a ConnectSphere account, you can safely ignore this email."
                );
            }
            case "LOGIN" -> {
                subject = "ConnectSphere - Your One-Time Login Code";
                htmlBody = buildOtpHtml(
                        "Your Login Code",
                        "Use the code below to sign in to your ConnectSphere account.",
                        otp,
                        "This code expires in <b>10 minutes</b>. Do not share it with anyone.",
                        "If you did not request this code, someone may be trying to access your account. Change your password immediately."
                );
            }
            case "RESET" -> {
                subject = "ConnectSphere - Password Reset Code";
                htmlBody = buildOtpHtml(
                        "Reset Your Password",
                        "We received a request to reset the password for your ConnectSphere account.",
                        otp,
                        "This code expires in <b>10 minutes</b>.",
                        "If you did not request a password reset, your account is safe."
                );
            }
            default -> {
                subject = "ConnectSphere - Verification Code";
                htmlBody = buildOtpHtml(
                        "Your Verification Code",
                        "Use the code below to complete your action on ConnectSphere.",
                        otp,
                        "This code expires in <b>10 minutes</b>.",
                        "If you did not request this code, please ignore this email."
                );
            }
        }

        boolean sent = sendHtmlEmail(to, subject, htmlBody);
        if (sent) {
            log.info("OTP email sent successfully to {}", to);
        } else {
            log.error("OTP email delivery failed for {} (purpose={})", to, purpose);
        }
        return sent;
    }

    @Async
    public void sendEmail(String to, String subject, String textBody) {
        String htmlBody = buildNotificationHtml(subject, textBody);
        sendHtmlEmail(to, subject, htmlBody);
    }

    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!isSmtpConfigured()) {
            log.warn("SMTP is not configured. Skipping email delivery to {} with subject '{}'", to, subject);
            return false;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            log.info("Email sent to {} | Subject: {}", to, subject);
            return true;
        } catch (MessagingException ex) {
            log.error("Failed to compose email for {} | Subject: {}", to, subject, ex);
        } catch (MailException ex) {
            log.error("SMTP send failed for {} | Subject: {}", to, subject, ex);
        } catch (Exception ex) {
            log.error("Unexpected email send failure for {} | Subject: {}", to, subject, ex);
        }

        return false;
    }

    private boolean isSmtpConfigured() {
        String username = fromEmail == null ? "" : fromEmail.trim();
        String password = smtpPassword == null ? "" : smtpPassword.trim();
        return !username.isEmpty() && !password.isEmpty();
    }

    private String buildOtpHtml(String title, String intro, String otp, String expiry, String footer) {
        return """
            <!DOCTYPE html>
            <html lang=\"en\">
            <head>
              <meta charset=\"UTF-8\"/>
              <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>
              <title>%s</title>
            </head>
            <body style=\"margin:0;padding:0;background:#f4f4f5;font-family:'Segoe UI',Arial,sans-serif;\">
              <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f4f5;padding:40px 0;\">
                <tr><td align=\"center\">
                  <table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);\">
                    <tr>
                      <td style=\"background:linear-gradient(135deg,#405DE6,#5851DB,#833AB4,#C13584,#E1306C,#FD1D1D);padding:32px;text-align:center;\">
                        <h1 style=\"margin:0;color:#ffffff;font-size:26px;font-weight:700;letter-spacing:-0.5px;\">ConnectSphere</h1>
                        <p style=\"margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:14px;\">Social Media Platform</p>
                      </td>
                    </tr>
                    <tr>
                      <td style=\"padding:40px 48px 32px;\">
                        <h2 style=\"margin:0 0 12px;color:#1a1a2e;font-size:22px;font-weight:700;\">%s</h2>
                        <p style=\"margin:0 0 28px;color:#555;font-size:15px;line-height:1.6;\">%s</p>
                        <div style=\"background:#f8f7ff;border:2px dashed #833AB4;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;\">
                          <p style=\"margin:0 0 8px;color:#888;font-size:12px;text-transform:uppercase;letter-spacing:2px;font-weight:600;\">Your Verification Code</p>
                          <span style=\"font-size:42px;font-weight:800;letter-spacing:12px;color:#1a1a2e;font-family:'Courier New',monospace;\">%s</span>
                        </div>
                        <p style=\"margin:0 0 28px;color:#888;font-size:13px;text-align:center;\">%s</p>
                        <hr style=\"border:none;border-top:1px solid #f0f0f0;margin:0 0 24px;\"/>
                        <p style=\"margin:0;color:#aaa;font-size:12px;line-height:1.6;\">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td style=\"background:#fafafa;padding:20px 48px;border-top:1px solid #f0f0f0;\">
                        <p style=\"margin:0;color:#bbb;font-size:12px;text-align:center;\">
                          Copyright 2026 ConnectSphere. All rights reserved.<br/>
                          This is an automated email - please do not reply.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, title, intro, otp, expiry, footer);
    }

    private String buildNotificationHtml(String title, String body) {
        String safeBody = body.replace("\n", "<br/>");
        return """
            <!DOCTYPE html>
            <html lang=\"en\">
            <head>
              <meta charset=\"UTF-8\"/>
              <title>%s</title>
            </head>
            <body style=\"margin:0;padding:0;background:#f4f4f5;font-family:'Segoe UI',Arial,sans-serif;\">
              <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f4f5;padding:40px 0;\">
                <tr><td align=\"center\">
                  <table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);\">
                    <tr>
                      <td style=\"background:linear-gradient(135deg,#405DE6,#5851DB,#833AB4,#C13584,#E1306C,#FD1D1D);padding:28px;text-align:center;\">
                        <h1 style=\"margin:0;color:#ffffff;font-size:22px;font-weight:700;\">ConnectSphere</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style=\"padding:36px 48px;\">
                        <h2 style=\"margin:0 0 16px;color:#1a1a2e;font-size:20px;\">%s</h2>
                        <p style=\"margin:0;color:#555;font-size:15px;line-height:1.7;\">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td style=\"background:#fafafa;padding:18px 48px;border-top:1px solid #f0f0f0;\">
                        <p style=\"margin:0;color:#bbb;font-size:12px;text-align:center;\">Copyright 2026 ConnectSphere - Do not reply to this email.</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, title, safeBody);
    }
}
