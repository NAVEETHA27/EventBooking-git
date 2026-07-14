package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Rating and review submitted by a verified attendee (QR check-in completed).
 * Only one review allowed per user per event.
 */
@Entity
@Table(name = "event_ratings", indexes = {
        @Index(name = "idx_rating_event", columnList = "event_id"),
        @Index(name = "idx_rating_user", columnList = "user_id"),
        @Index(name = "idx_rating_verified", columnList = "is_verified_attendance")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_rating_user_event", columnNames = {"user_id", "event_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Sub-ratings (1–5)
    @Min(1) @Max(5)
    @Column(name = "overall_rating", nullable = false)
    private int overallRating;

    @Min(1) @Max(5)
    @Column(name = "speaker_rating")
    private Integer speakerRating;

    @Min(1) @Max(5)
    @Column(name = "event_quality_rating")
    private Integer eventQualityRating;

    @Min(1) @Max(5)
    @Column(name = "venue_rating")
    private Integer venueRating;

    @Min(1) @Max(5)
    @Column(name = "food_rating")
    private Integer foodRating;

    @Min(1) @Max(5)
    @Column(name = "organization_rating")
    private Integer organizationRating;

    @Min(1) @Max(5)
    @Column(name = "accommodation_rating")
    private Integer accommodationRating;

    @Min(1) @Max(5)
    @Column(name = "time_management_rating")
    private Integer timeManagementRating;

    @Min(1) @Max(5)
    @Column(name = "content_quality_rating")
    private Integer contentQualityRating;

    @Min(1) @Max(5)
    @Column(name = "value_for_money_rating")
    private Integer valueForMoneyRating;

    @Size(max = 2000)
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Size(max = 2000)
    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "anonymous", nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    /** Comma-separated URLs of uploaded event photos */
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(name = "is_verified_attendance", nullable = false)
    @Builder.Default
    private boolean verifiedAttendance = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", length = 20)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(name = "moderation_note", length = 500)
    private String moderationNote;

    @Column(name = "is_fake_flagged")
    @Builder.Default
    private boolean fakeFlagged = false;

    /** AI-generated sentiment: POSITIVE / NEUTRAL / NEGATIVE */
    @Column(name = "sentiment", length = 20)
    private String sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    /** Whether review is an AI-generated testimonial candidate */
    @Column(name = "is_testimonial_candidate")
    @Builder.Default
    private boolean testimonialCandidate = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ModerationStatus {
        PENDING, APPROVED, REJECTED, FLAGGED
    }
}
