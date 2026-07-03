package com.eventbooking.controller;

import com.eventbooking.dto.request.*;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.AuthResponse;
import com.eventbooking.entity.Organizer;
import com.eventbooking.entity.User;
import com.eventbooking.repository.OrganizerRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.JwtTokenProvider;
import com.eventbooking.security.OtpStore;
import com.eventbooking.security.RefreshTokenStore;
import com.eventbooking.service.AuthService;
import com.eventbooking.service.EmailService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService         authService;
    private final JwtTokenProvider    jwtTokenProvider;
    private final RefreshTokenStore   refreshTokenStore;
    private final OtpStore            otpStore;
    private final EmailService        emailService;
    private final UserRepository      userRepository;
    private final OrganizerRepository organizerRepository;

    @Value("${spring.mail.username:NOT_CONFIGURED}")
    private String mailUsername;

    // ── User ──────────────────────────────────────────────────────────────

    @PostMapping("/user/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(
            @Valid @RequestBody UserRegisterRequest request) {
        AuthResponse auth = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful! Please verify your email.", auth));
    }

    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.loginUser(request)));
    }

    // ── Organizer ─────────────────────────────────────────────────────────

    @PostMapping("/organizer/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerOrganizer(
            @Valid @RequestBody OrganizerRegisterRequest request) {
        AuthResponse auth = authService.registerOrganizer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organizer registration successful! Please verify your email.", auth));
    }

    @PostMapping("/organizer/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginOrganizer(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.loginOrganizer(request)));
    }

    // ── Email Verification ─────────────────────────────────────────────────

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token,
            @RequestParam String role) {
        authService.verifyEmail(token, role);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully!", null));
    }

    // ── Password Recovery ──────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully.", null));
    }

    @PostMapping("/reset-password/otp")
    public ResponseEntity<ApiResponse<Void>> resetPasswordWithOtp(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String role = body.getOrDefault("role", "USER");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || email.isBlank() || otp == null || otp.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("email, otp and newPassword are required"));
        }

        authService.resetPasswordWithOtp(email, role, otp, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully.", null));
    }

    // ── Token Refresh ──────────────────────────────────────────────────────

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired refresh token"));

        Claims claims = jwtTokenProvider.extractClaims(request.refreshToken());
        String jti = claims.getId();
        if (jti == null || refreshTokenStore.isUsed(jti))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("TOKEN_ALREADY_USED"));

        refreshTokenStore.markUsed(jti);

        Long   id    = claims.get("id", Long.class);
        String email = claims.getSubject();
        String role  = claims.get("role", String.class);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(id, email, role))
                .refreshToken(jwtTokenProvider.generateRefreshToken(id, email, role))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    // ── OTP: Send ──────────────────────────────────────────────────────────

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String role = body.getOrDefault("role", "USER");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
        }

        String otp = otpStore.generate(email);
        log.info("[OTP] Generated for email={}", email);

        String name = body.getOrDefault("name", "User");
        try {
            if ("ORGANIZER".equalsIgnoreCase(role)) {
                Organizer org = organizerRepository.findByEmail(email).orElse(null);
                if (org != null) name = org.getOrganizerName();
            } else {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) name = user.getName();
            }
        } catch (Exception ignored) {}

        try {
            emailService.sendOtpEmail(email, name, otp);
            log.info("[OTP] Email sent to {} via SMTP {}", email, mailUsername);
            return ResponseEntity.ok(ApiResponse.success("OTP sent to " + email,
                    Map.of("emailSent", true)));
        } catch (Exception emailEx) {
            log.warn("[OTP] Email delivery failed for {}. Cause: {}", email, emailEx.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(
                    "Could not send OTP email. Configure MAIL_USERNAME and MAIL_PASSWORD " +
                    "(Gmail App Password) and try again."));
        }
    }

    // OTP: Verify ────────────────────────────────────────────────────────

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("email and otp are required"));

        log.info("🔍 [OTP] Verify attempt for email={}", email);
        boolean valid = otpStore.verify(email, otp);

        if (!valid) {
            log.warn("❌ [OTP] Verification FAILED for email={} (wrong, expired, or too many attempts)", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid or expired OTP. Please request a new one."));
        }

        log.info("✅ [OTP] Verified successfully for email={}", email);
        authService.markEmailVerifiedByOtp(email, body.getOrDefault("role", "USER"));
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully",
                Map.of("verified", true, "emailVerified", true)));
    }

    // ── Test Email (dev only) ─────────────────────────────────────────────
    @GetMapping("/test-email")
    @Profile("dev")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testEmail(
            @RequestParam(defaultValue = "") String to) {
        String target = to.isBlank() ? mailUsername : to;

        if (target.isBlank() || target.equals("NOT_CONFIGURED")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "No email address configured. " +
                    "Set MAIL_USERNAME env var or pass ?to=youraddress@gmail.com"));
        }

        log.info("📧 [TEST] Sending test email to: {}", target);
        try {
            emailService.sendOtpEmail(target, "Test User", "123456");
            log.info("✅ [TEST] Test email delivered to {}", target);
            return ResponseEntity.ok(ApiResponse.success(
                    "Test email sent successfully to " + target,
                    Map.of("recipient", target, "status", "delivered")));
        } catch (Exception ex) {
            log.error("❌ [TEST] Test email FAILED to {}: {}", target, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                    "Email delivery failed: " + ex.getMessage() +
                    " — check application.yml SMTP settings and ensure Gmail App Password is set."));
        }
    }
}
