package com.eventbooking.service;

import com.eventbooking.dto.request.*;
import com.eventbooking.dto.response.AuthResponse;
import com.eventbooking.exception.*;
import com.eventbooking.entity.Organizer;
import com.eventbooking.entity.ProfileLocation;
import com.eventbooking.entity.User;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.OrganizerRepository;
import com.eventbooking.repository.ProfileLocationRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.JwtTokenProvider;
import com.eventbooking.security.OtpStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles all authentication: registration, login, email verification,
 * forgot/reset password for both Users and Organizers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository       userRepository;
    private final OrganizerRepository  organizerRepository;
    private final JwtTokenProvider     jwtTokenProvider;
    private final ProfileLocationRepository profileLocationRepository;
    private final OtpStore             otpStore;
    private final PasswordEncoder      passwordEncoder;
    private final EmailService         emailService;
    private final NotificationService  notificationService;
    private final AuditService auditService;

    // ─── USER REGISTRATION ────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerUser(UserRegisterRequest request) {
        // Sanitise inputs — strip leading/trailing whitespace (Req 9.1)
        String email = StringUtils.trimWhitespace(request.getEmail());
        String name  = StringUtils.trimWhitespace(request.getName());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User Already Exists with email: " + email);
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .pinCode(request.getPinCode())
                .organizationName(request.getOrganizationName())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .emailVerified(false)
                .verificationToken(verificationToken)
                .role(User.UserRole.USER)
                .build();

        user = userRepository.saveAndFlush(user);
        user.setUserCode("USR" + String.format("%04d", user.getId()));
        user = userRepository.save(user);
        saveInitialUserLocation(user.getId(), request);
        log.info("New user registered: {}", user.getEmail());

        notificationService.sendNotification(user.getId(), "USER",
                "REGISTRATION_SUCCESS", "Welcome to EventBooking!",
                "Your account has been created. Please verify your email.", null);
        auditService.record(user.getId(), "USER", "REGISTRATION", "USER",
                String.valueOf(user.getId()), "User registered");

        return buildAuthResponse(user.getId(), user.getEmail(), user.getRole().name(),
                user.getName(), user.getProfilePicture(), user.isEmailVerified());
    }

    // ─── USER LOGIN ───────────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid Credentials"));

        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is locked. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            userRepository.incrementFailedAttempts(user.getId());
            if (user.getFailedLoginAttempts() + 1 >= 5) {
                user.setAccountLocked(true);
                userRepository.save(user);
                throw new AccountLockedException("Account locked after too many failed attempts.");
            }
            throw new InvalidCredentialsException("Invalid Credentials");
        }

        userRepository.resetFailedAttempts(user.getId());

        notificationService.sendNotification(user.getId(), "USER",
                "LOGIN_ALERT", "New Login Detected",
                "A new login was detected on your account.", null);
        auditService.record(user.getId(), "USER", "LOGIN", "USER",
                String.valueOf(user.getId()), "User login");

        return buildAuthResponse(user.getId(), user.getEmail(), user.getRole().name(),
                user.getName(), user.getProfilePicture(), user.isEmailVerified());
    }

    // ─── ORGANIZER REGISTRATION ───────────────────────────────────────────

    @Transactional
    public AuthResponse registerOrganizer(OrganizerRegisterRequest request) {
        if (organizerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Organizer Already Exists with email: " + request.getEmail());
        }

        String verificationToken = UUID.randomUUID().toString();

        Organizer organizer = Organizer.builder()
                .organizerName(request.getOrganizerName())
                .organizationName(request.getOrganizationName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .pinCode(request.getPinCode())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .website(request.getWebsite())
                .description(request.getDescription())
                .emailVerified(false)
                .verificationToken(verificationToken)
                .role(Organizer.OrganizerRole.ORGANIZER)
                .build();

        organizer = organizerRepository.saveAndFlush(organizer);
        organizer.setOrganizerCode("ORG" + String.format("%04d", organizer.getId()));
        organizer = organizerRepository.save(organizer);
        log.info("New organizer registered: {}", organizer.getEmail());

        notificationService.sendNotification(organizer.getId(), "ORGANIZER",
                "REGISTRATION_SUCCESS", "Welcome Organizer!",
                "Your organizer account has been created.", null);
        auditService.record(organizer.getId(), "ORGANIZER", "REGISTRATION", "ORGANIZER",
                String.valueOf(organizer.getId()), "Organizer registered");

        return buildAuthResponse(organizer.getId(), organizer.getEmail(),
                organizer.getRole().name(), organizer.getOrganizerName(),
                organizer.getOrganizationLogo(), organizer.isEmailVerified());
    }

    // ─── ORGANIZER LOGIN ──────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginOrganizer(LoginRequest request) {
        Organizer organizer = organizerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid Credentials"));

        if (!passwordEncoder.matches(request.getPassword(), organizer.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid Credentials");
        }
        auditService.record(organizer.getId(), "ORGANIZER", "LOGIN", "ORGANIZER",
                String.valueOf(organizer.getId()), "Organizer login");

        return buildAuthResponse(organizer.getId(), organizer.getEmail(),
                organizer.getRole().name(), organizer.getOrganizerName(),
                organizer.getOrganizationLogo(), organizer.isEmailVerified());
    }

    // ─── EMAIL VERIFICATION (OTP) ─────────────────────────────────────────

    @Transactional
    public void markEmailVerifiedByOtp(String email, String role) {
        if ("ORGANIZER".equalsIgnoreCase(role)) {
            Organizer org = organizerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
            org.setEmailVerified(true);
            org.setVerificationToken(null);
            organizerRepository.save(org);
        } else {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
        }
    }

    // ─── EMAIL VERIFICATION (link token) ──────────────────────────────────

    @Transactional
    public void verifyEmail(String token, String role) {
        if ("USER".equalsIgnoreCase(role)) {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
        } else {
            Organizer org = organizerRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));
            org.setEmailVerified(true);
            org.setVerificationToken(null);
            organizerRepository.save(org);
        }
    }

    // ─── FORGOT PASSWORD ──────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        if ("USER".equalsIgnoreCase(request.getRole())) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("No account found with that email"));
            user.setResetPasswordToken(resetToken);
            user.setResetTokenExpiry(expiry);
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
        } else {
            Organizer org = organizerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("No account found with that email"));
            org.setResetPasswordToken(resetToken);
            org.setResetTokenExpiry(expiry);
            organizerRepository.save(org);
            emailService.sendPasswordResetEmail(org.getEmail(), org.getOrganizerName(), resetToken);
        }
    }

    // ─── RESET PASSWORD ───────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if ("USER".equalsIgnoreCase(request.getRole())) {
            User user = userRepository.findByResetPasswordToken(request.getToken())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));
            if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new TokenExpiredException("Password reset token has expired");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setResetPasswordToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
        } else {
            Organizer org = organizerRepository.findByResetPasswordToken(request.getToken())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));
            if (org.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new TokenExpiredException("Password reset token has expired");
            }
            org.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            org.setResetPasswordToken(null);
            org.setResetTokenExpiry(null);
            organizerRepository.save(org);
        }
    }

    @Transactional
    public void resetPasswordWithOtp(String email, String role, String otp, String newPassword) {
        if (!otpStore.verify(email, otp)) {
            throw new InvalidCredentialsException("Invalid or expired OTP. Please request a new one.");
        }

        if ("USER".equalsIgnoreCase(role)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No account found with that email"));
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setResetPasswordToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
        } else {
            Organizer org = organizerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No account found with that email"));
            org.setPasswordHash(passwordEncoder.encode(newPassword));
            org.setResetPasswordToken(null);
            org.setResetTokenExpiry(null);
            organizerRepository.save(org);
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(Long id, String email, String role,
                                            String name, String picture, boolean verified) {
        String accessToken  = jwtTokenProvider.generateToken(id, email, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(id, email, role);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .expiresAt(System.currentTimeMillis() + jwtTokenProvider.getExpirationMs())
                .user(AuthResponse.UserInfo.builder()
                        .id(id)
                        .name(name)
                        .email(email)
                        .role(role)
                        .profilePicture(picture)
                        .emailVerified(verified)
                        .build())
                .build();
    }

    private void saveInitialUserLocation(Long userId, UserRegisterRequest request) {
        boolean hasLocation =
                StringUtils.hasText(request.getAddress()) ||
                StringUtils.hasText(request.getStreet()) ||
                StringUtils.hasText(request.getArea()) ||
                StringUtils.hasText(request.getCity()) ||
                StringUtils.hasText(request.getDistrict()) ||
                StringUtils.hasText(request.getState()) ||
                StringUtils.hasText(request.getCountry()) ||
                StringUtils.hasText(request.getPinCode()) ||
                request.getLatitude() != null ||
                request.getLongitude() != null;

        if (!hasLocation) return;

        ProfileLocation location = ProfileLocation.builder()
                .userId(userId)
                .address(request.getAddress())
                .street(request.getStreet())
                .area(request.getArea())
                .city(request.getCity())
                .district(request.getDistrict())
                .state(request.getState())
                .country(request.getCountry())
                .pinCode(request.getPinCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        profileLocationRepository.save(location);
    }
}
