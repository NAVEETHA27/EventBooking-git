package com.eventbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Certificate issued to a participant who completed an event (attendance verified).
 */
@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_cert_event", columnList = "event_id"),
        @Index(name = "idx_cert_user", columnList = "user_id"),
        @Index(name = "idx_cert_code", columnList = "certificate_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_id", nullable = false, unique = true, length = 50)
    private String certificateId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "college_name", length = 200)
    private String collegeName;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "verification_token", nullable = false, unique = true, length = 80)
    @Builder.Default
    private String verificationToken = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", length = 30)
    @Builder.Default
    private CertificateSettings.CertificateType certificateType = CertificateSettings.CertificateType.PARTICIPATION;

    @Column(name = "participant_id", length = 80)
    private String participantId;

    @Column(name = "participant_email", length = 160)
    private String participantEmail;

    @Column(name = "organizer_name", length = 160)
    private String organizerName;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Column(name = "verification_url", length = 500)
    private String verificationUrl;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private long downloadCount = 0;

    @Column(name = "verification_count", nullable = false)
    @Builder.Default
    private long verificationCount = 0;

    @Column(name = "email_attempt_count", nullable = false)
    @Builder.Default
    private int emailAttemptCount = 0;

    @Column(name = "email_failure_reason", length = 500)
    private String emailFailureReason;

    @Column(name = "email_sent")
    @Builder.Default
    private boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum CertificateStatus {
        PENDING, GENERATED, RELEASED, EMAILED, FAILED, REVOKED
    }
}
