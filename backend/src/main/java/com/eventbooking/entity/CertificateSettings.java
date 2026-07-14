package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_settings", indexes = {
        @Index(name = "idx_cert_settings_event", columnList = "event_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificateSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "certificate_available", nullable = false)
    @Builder.Default
    private boolean certificateAvailable = false;

    @Column(name = "automatic_generation", nullable = false)
    @Builder.Default
    private boolean automaticGeneration = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_mode", length = 30, nullable = false)
    @Builder.Default
    private ReleaseMode releaseMode = ReleaseMode.MANUAL;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "minimum_attendance_required", nullable = false)
    @Builder.Default
    private boolean minimumAttendanceRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", length = 30, nullable = false)
    @Builder.Default
    private CertificateType certificateType = CertificateType.PARTICIPATION;

    @Column(name = "organizer_signature_url", length = 500)
    private String organizerSignatureUrl;

    @Column(name = "organizer_name", length = 160)
    private String organizerName;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Column(name = "verification_base_url", length = 500)
    private String verificationBaseUrl;

    @Column(name = "certificate_expiry")
    private LocalDate certificateExpiry;

    @Column(name = "theme", length = 40)
    @Builder.Default
    private String theme = "MODERN_BLUE";

    @Column(name = "released", nullable = false)
    @Builder.Default
    private boolean released = false;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public enum ReleaseMode {
        IMMEDIATE_AFTER_EVENT, MANUAL, SCHEDULED
    }

    public enum CertificateType {
        PARTICIPATION, WINNER, RUNNER_UP, VOLUNTEER, ORGANIZER, SPEAKER
    }
}
