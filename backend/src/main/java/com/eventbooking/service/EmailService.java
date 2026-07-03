package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.User;
import com.eventbooking.entity.mongo.EmailLog;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.repository.mongo.EmailLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sends HTML emails for OTP verification, password reset, and booking confirmation.
 *
 * Configuration required in application.yml:
 *   spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password
 *
 * For Gmail: use an App Password (Google Account → Security → App Passwords).
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Required injection — will throw at startup if spring.mail.* is not configured.
     * Use @Autowired(required=false) only if you want silent dev mode.
     */
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmailLogRepository emailLogRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:NOT_CONFIGURED}")
    private String mailUsername;

    /** Startup validation — confirms JavaMailSender is wired and logs SMTP config. */
    @PostConstruct
    public void validateMailConfig() {
        if (mailSender instanceof JavaMailSenderImpl impl) {
            log.info("✅ JavaMailSender initialized — SMTP host: {}:{}, from: {}",
                    impl.getHost(), impl.getPort(), mailUsername);
        } else {
            log.info("✅ JavaMailSender initialized (custom implementation)");
        }

        // Warn if placeholder credentials are still set
        if (mailUsername.contains("REPLACE_WITH") || mailUsername.equals("NOT_CONFIGURED")) {
            log.warn("⚠️  Mail username looks like a placeholder: '{}'. " +
                     "Set MAIL_USERNAME and MAIL_PASSWORD environment variables " +
                     "or replace values in application.yml to enable email delivery.", mailUsername);
        }
    }

    // ── OTP Verification ──────────────────────────────────────────────────

    public void sendOtpEmail(String to, String name, String otp) {
        log.info("📧 Sending OTP email to: {}", to);
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
              <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🎓 CollegeEvents</h1>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 8px 8px;text-align:center">
                <h2 style="color:#1565C0">Hi %s,</h2>
                <p style="color:#555">Use the OTP below to verify your account.<br>
                   It expires in <strong>10 minutes</strong>.</p>
                <div style="display:inline-block;background:#F0F4FF;border:2px dashed #1565C0;
                            padding:18px 40px;border-radius:12px;margin:20px 0">
                  <span style="font-size:36px;font-weight:900;letter-spacing:10px;color:#1565C0">%s</span>
                </div>
                <p style="color:#999;font-size:12px;margin-top:16px">
                  Never share this OTP. If you did not request this, please ignore this email.
                </p>
              </div>
            </div>
            """.formatted(name, otp);
        sendHtmlEmail(to, "Your Verification OTP – CollegeEvents", html);
    }

    // ── Email Verification ────────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String to, String name, String token, String role) {
        String link = frontendUrl + "/verify-email?token=" + token + "&role=" + role;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
              <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🎓 CollegeEvents</h1>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 8px 8px">
                <h2>Hello %s!</h2>
                <p>Thank you for registering. Please verify your email address.</p>
                <a href="%s" style="display:inline-block;background:#1565C0;color:white;padding:14px 28px;
                   border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0">
                  Verify Email Address
                </a>
                <p style="color:#666;font-size:13px">Link expires in 24 hours.</p>
              </div>
            </div>
            """.formatted(name, link);
        sendHtmlEmail(to, "Verify Your Email – CollegeEvents", html);
    }

    // ── Password Reset ────────────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
              <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🎓 CollegeEvents</h1>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 8px 8px">
                <h2>Hi %s,</h2>
                <p>We received a request to reset your password.</p>
                <a href="%s" style="display:inline-block;background:#D32F2F;color:white;padding:14px 28px;
                   border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0">
                  Reset Password
                </a>
                <p style="color:#666;font-size:13px">This link expires in 1 hour.</p>
              </div>
            </div>
            """.formatted(name, link);
        sendHtmlEmail(to, "Reset Your Password – CollegeEvents", html);
    }

    // ── Account Locked ────────────────────────────────────────────────────

    @Async
    public void sendAccountLockedEmail(String to, String name) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
              <div style="background:#D32F2F;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🔒 Account Locked</h1>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 8px 8px">
                <h2>Hi %s,</h2>
                <p>Your account has been <strong>locked</strong> due to 5 consecutive failed login attempts.</p>
                <p>Use the password reset flow to regain access.</p>
              </div>
            </div>
            """.formatted(name);
        sendHtmlEmail(to, "Account Locked – CollegeEvents", html);
    }

    // ── Booking Confirmation ──────────────────────────────────────────────

    @Async
    public void sendBookingConfirmation(String to, String name, Booking booking, Event event) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
              <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🎫 Booking Confirmed!</h1>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 8px 8px">
                <h2>Hello %s!</h2>
                <div style="background:#f0f4ff;padding:20px;border-radius:8px;margin:16px 0">
                  <p><strong>Event:</strong> %s</p>
                  <p><strong>Date:</strong> %s at %s</p>
                  <p><strong>Venue:</strong> %s</p>
                  <p><strong>Ticket ID:</strong>
                    <code style="background:#1565C0;color:white;padding:4px 8px;border-radius:4px">%s</code>
                  </p>
                  <p><strong>Tickets:</strong> %d</p>
                  <p><strong>Total:</strong> ₹%.2f</p>
                </div>
              </div>
            </div>
            """.formatted(
                name, event.getEventName(),
                event.getEventDate(), event.getEventTime(),
                event.getVenueName() != null ? event.getVenueName() : event.getLocation(),
                booking.getTicketId(), booking.getQuantity(),
                booking.getTotalAmount().doubleValue());
        sendHtmlEmail(to, "Booking Confirmed – " + event.getEventName(), html);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    @Async
    public void sendNewEventNotification(Event event) {
        Optional<EmailLogRepository> emailLogs = Optional.ofNullable(emailLogRepository);
        if (emailLogs.isEmpty()) {
            log.warn("MongoDB is disabled or unavailable; email delivery will continue without EmailLog persistence");
        }
        for (User user : userRepository.findAll()) {
            EmailLog logEntry = EmailLog.builder()
                    .to(user.getEmail())
                    .subject("New College Event Available")
                    .status("QUEUED")
                    .eventId(event.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            if (emailLogs.isPresent()) {
                logEntry = emailLogs.get().save(logEntry);
            }
            try {
                String link = frontendUrl + "/events/" + event.getId();
                String html = """
                    <div style="font-family:Arial,sans-serif;max-width:640px;margin:auto;padding:24px;background:#f9f9f9">
                      <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                        <h1 style="color:white;margin:0">New College Event Available</h1>
                      </div>
                      <div style="background:white;padding:28px;border-radius:0 0 8px 8px">
                        <h2 style="color:#1565C0">%s</h2>
                        <p><strong>Date:</strong> %s</p>
                        <p><strong>Time:</strong> %s</p>
                        <p><strong>Venue:</strong> %s</p>
                        <p><strong>Category:</strong> %s</p>
                        <p style="color:#555">%s</p>
                        <a href="%s" style="display:inline-block;background:#1565C0;color:white;padding:12px 22px;border-radius:8px;text-decoration:none;font-weight:bold">Register Now</a>
                      </div>
                    </div>
                    """.formatted(
                        event.getEventName(),
                        event.getEventDate(),
                        event.getEventTime(),
                        event.getVenueName() != null ? event.getVenueName() : event.getLocation(),
                        event.getCategory(),
                        event.getDescription() != null ? event.getDescription() : "",
                        link);
                sendHtmlEmail(user.getEmail(), "New College Event Available", html);
                logEntry.setStatus("SENT");
                logEntry.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(ex.getMessage());
                logEntry.setAttempts(logEntry.getAttempts() + 1);
            }
            if (emailLogs.isPresent()) {
                emailLogs.get().save(logEntry);
            }
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(mailUsername);
            mailSender.send(message);
            log.info("✅ Email sent to {}: {}", to, subject);
        } catch (Exception ex) {
            log.error("❌ Email delivery FAILED to {}: {} — cause: {}",
                    to, subject, ex.getMessage());
            // Re-throw as runtime so caller knows if needed
            throw new RuntimeException("Email delivery failed: " + ex.getMessage(), ex);
        }
    }
}
