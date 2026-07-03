package com.eventbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a registered end-user.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_public_id", columnList = "user_code", unique = true),
        @Index(name = "idx_user_verified", columnList = "email_verified")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"passwordHash", "bookings"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", unique = true, updatable = false, length = 20)
    private String userCode;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be 2–120 characters")
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number")
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "pin_code", length = 20)
    private String pinCode;

    @Column(name = "organization_name", length = 160)
    private String organizationName;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 120)
    private String verificationToken;

    @Column(name = "reset_password_token", length = 120)
    private String resetPasswordToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    public enum UserRole {
        USER, ADMIN
    }
}
