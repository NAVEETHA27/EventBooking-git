package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a college event.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_status",     columnList = "status"),
        @Index(name = "idx_event_date",       columnList = "event_date"),
        @Index(name = "idx_event_category",   columnList = "category"),
        @Index(name = "idx_event_type",       columnList = "event_type"),
        @Index(name = "idx_event_college",    columnList = "college_name"),
        @Index(name = "idx_event_department", columnList = "department_name"),
        @Index(name = "idx_event_organizer",  columnList = "organizer_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    // ── Basic Info ────────────────────────────────────────────────────────
    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 200)
    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Category — college-focused:
     * HACKATHON, TECHNICAL_SYMPOSIUM, CODING_COMPETITION, WORKSHOP, SEMINAR,
     * PROJECT_EXHIBITION, PLACEMENT_PREP, TECHNICAL_TRAINING,
     * CULTURAL, SPORTS, CLUB_ACTIVITY, DEPARTMENT_FUNCTION,
     * INTER_COLLEGE, INTRA_COLLEGE, OTHER
     */
    @NotBlank(message = "Category is required")
    @Column(name = "category", nullable = false, length = 80)
    private String category;

    /** ONLINE / OFFLINE / HYBRID */
    @NotBlank(message = "Event type is required")
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    // ── College Details ───────────────────────────────────────────────────
    @Column(name = "college_name", length = 200)
    private String collegeName;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    // ── Media ─────────────────────────────────────────────────────────────
    @Column(name = "event_banner")
    private String eventBanner;

    @Column(name = "event_images", columnDefinition = "TEXT")
    private String eventImages;

    @Column(name = "authorized_document_url", length = 500)
    private String authorizedDocumentUrl;

    // ── Schedule ──────────────────────────────────────────────────────────
    @NotNull(message = "Event date is required")
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @NotNull(message = "Event time is required")
    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    // ── Location ──────────────────────────────────────────────────────────
    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "location", length = 300)
    private String location;

    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;

    // ── Tickets ───────────────────────────────────────────────────────────
    @DecimalMin(value = "0.00")
    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ticketPrice = BigDecimal.ZERO;

    @Min(value = 1)
    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    // ── Status & Visibility ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.PUBLIC;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean featured = false;

    // ── Extra ─────────────────────────────────────────────────────────────
    @Column(name = "tags", length = 400)
    private String tags;

    @Column(name = "organizer_details", columnDefinition = "TEXT")
    private String organizerDetails;

    /** Whether attendees earn a certificate */
    @Column(name = "has_certificate")
    @Builder.Default
    private boolean hasCertificate = false;

    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;

    // ── Audit ─────────────────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum EventStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, PUBLISHED, UPCOMING, ONGOING, COMPLETED, CANCELLED, EXPIRED
    }

    public enum EventVisibility {
        PUBLIC, PRIVATE
    }
}
