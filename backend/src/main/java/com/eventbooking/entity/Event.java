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

    @Column(name = "whatsapp_group_link", length = 500)
    private String whatsappGroupLink;

    @Column(name = "whatsapp_contact_number", length = 30)
    private String whatsappContactNumber;

    /** Venue latitude for proximity search and AI travel assistant */
    @Column(name = "venue_latitude")
    private Double venueLatitude;

    /** Venue longitude for proximity search and AI travel assistant */
    @Column(name = "venue_longitude")
    private Double venueLongitude;

    @Column(name = "estimated_travel_time", length = 120)
    private String estimatedTravelTime;

    @Column(name = "cab_estimate", length = 120)
    private String cabEstimate;

    @Column(name = "nearby_hotels", columnDefinition = "TEXT")
    private String nearbyHotels;

    @Column(name = "nearby_restaurants", columnDefinition = "TEXT")
    private String nearbyRestaurants;

    @Column(name = "emergency_contacts", columnDefinition = "TEXT")
    private String emergencyContacts;

    // ── Food & Accommodation ──────────────────────────────────────────────
    @Column(name = "food_provided", nullable = false)
    @Builder.Default
    private Boolean foodProvided = false;

    /** Comma-separated meals: BREAKFAST, LUNCH, DINNER, SNACKS */
    @Column(name = "food_meals", length = 200)
    private String foodMeals;

    /** VEG / NON_VEG / BOTH */
    @Column(name = "food_type", length = 20)
    private String foodType;

    @Column(name = "tea_coffee_provided", nullable = false)
    @Builder.Default
    private Boolean teaCoffeeProvided = false;

    @Column(name = "special_diet", length = 300)
    private String specialDiet;

    @Column(name = "accommodation_provided", nullable = false)
    @Builder.Default
    private Boolean accommodationProvided = false;

    /** HOSTEL / HOTEL / BOTH */
    @Column(name = "accommodation_type", length = 30)
    private String accommodationType;

    @Column(name = "accommodation_charges")
    private Integer accommodationCharges;

    @Column(name = "accommodation_beds_available")
    private Integer accommodationBedsAvailable;

    @Column(name = "accommodation_details", columnDefinition = "TEXT")
    private String accommodationDetails;

    @Column(name = "boys_hostel_available", nullable = false)
    @Builder.Default
    private Boolean boysHostelAvailable = false;

    @Column(name = "girls_hostel_available", nullable = false)
    @Builder.Default
    private Boolean girlsHostelAvailable = false;

    @Column(name = "hotel_tieup_available", nullable = false)
    @Builder.Default
    private Boolean hotelTieupAvailable = false;

    @Column(name = "accommodation_check_in", length = 80)
    private String accommodationCheckIn;

    @Column(name = "accommodation_check_out", length = 80)
    private String accommodationCheckOut;

    @Column(name = "accommodation_contact_person", length = 160)
    private String accommodationContactPerson;

    @Column(name = "session_schedule", columnDefinition = "TEXT")
    private String sessionSchedule;

    @Column(name = "speaker_list", columnDefinition = "TEXT")
    private String speakerList;

    @Column(name = "live_announcements", columnDefinition = "TEXT")
    private String liveAnnouncements;

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

    @Column(name = "certificate_template_url", length = 500)
    private String certificateTemplateUrl;

    @Column(name = "certificate_signature_name", length = 160)
    private String certificateSignatureName;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CertificateSettings certificateSettings;

    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;

    /** Date after which certificates are distributed (default: 7 days after event end) */
    @Column(name = "certificate_deadline")
    private LocalDate certificateDeadline;

    /** Timestamp when event went LIVE */
    @Column(name = "live_at")
    private LocalDateTime liveAt;

    /** Timestamp when event was marked COMPLETED */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Timestamp when event was marked EXPIRED (certificate deadline passed) */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // ── Transportation ────────────────────────────────────────────────────
    @Column(name = "nearest_bus_stop", length = 200)
    private String nearestBusStop;

    @Column(name = "distance_from_bus_stop", length = 120)
    private String distanceFromBusStop;

    @Column(name = "bus_numbers", length = 300)
    private String busNumbers;

    @Column(name = "nearest_railway_station", length = 200)
    private String nearestRailwayStation;

    @Column(name = "distance_from_railway_station", length = 120)
    private String distanceFromRailwayStation;

    @Column(name = "nearest_airport", length = 200)
    private String nearestAirport;

    @Column(name = "metro_information", length = 200)
    private String metroInformation;

    @Column(name = "landmarks", columnDefinition = "TEXT")
    private String landmarks;

    /** FREE / PAID / NONE / LIMITED */
    @Column(name = "parking_available", length = 20)
    private String parkingAvailable;

    @Column(name = "travel_guide", columnDefinition = "TEXT")
    private String travelGuide;

    // Important Information
    @Column(name = "reporting_time")
    private LocalTime reportingTime;

    @Column(name = "dress_code", length = 200)
    private String dressCode;

    @Column(name = "items_to_bring", columnDefinition = "TEXT")
    private String itemsToBring;

    @Column(name = "laptop_required", nullable = false)
    @Builder.Default
    private Boolean laptopRequired = false;

    @Column(name = "id_card_required", nullable = false)
    @Builder.Default
    private Boolean idCardRequired = false;

    @Column(name = "team_size", length = 80)
    private String teamSize;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "refund_policy", columnDefinition = "TEXT")
    private String refundPolicy;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "certificate_eligibility", columnDefinition = "TEXT")
    private String certificateEligibility;

    // Facility flags
    @Column(name = "wifi_available", nullable = false)
    @Builder.Default
    private Boolean wifiAvailable = false;

    @Column(name = "wheelchair_accessible", nullable = false)
    @Builder.Default
    private Boolean wheelchairAccessible = false;

    @Column(name = "rest_rooms_available", nullable = false)
    @Builder.Default
    private Boolean restRoomsAvailable = false;

    @Column(name = "drinking_water_available", nullable = false)
    @Builder.Default
    private Boolean drinkingWaterAvailable = false;

    @Column(name = "medical_support_available", nullable = false)
    @Builder.Default
    private Boolean medicalSupportAvailable = false;

    @Column(name = "networking_enabled", nullable = false)
    @Builder.Default
    private Boolean networkingEnabled = false;

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
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, PUBLISHED, UPCOMING, LIVE, ONGOING, COMPLETED, CANCELLED, EXPIRED
    }

    public enum EventVisibility {
        PUBLIC, PRIVATE
    }
}
