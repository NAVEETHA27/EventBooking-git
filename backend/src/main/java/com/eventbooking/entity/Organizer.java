package com.eventbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an event organizer.
 */
@Entity
@Table(name = "organizers", indexes = {
        @Index(name = "idx_organizer_email", columnList = "email", unique = true)
        ,@Index(name = "idx_organizer_public_id", columnList = "organizer_code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"passwordHash", "events"})
public class Organizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizer_code", unique = true, updatable = false, length = 30)
    private String organizerCode;

    @NotBlank(message = "Organizer name is required")
    @Size(min = 2, max = 120)
    @Column(name = "organizer_name", nullable = false, length = 120)
    private String organizerName;

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 160)
    @Column(name = "organization_name", nullable = false, length = 160)
    private String organizationName;

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

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "organization_logo")
    private String organizationLogo;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 120)
    private String verificationToken;

    @Column(name = "reset_password_token", length = 120)
    private String resetPasswordToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private boolean approved = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private OrganizerRole role = OrganizerRole.ORGANIZER;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    public enum OrganizerRole {
        ORGANIZER, ADMIN
    }
}
