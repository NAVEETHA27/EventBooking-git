package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI-computed performance score and badge for an organizer.
 */
@Entity
@Table(name = "organizer_scores", indexes = {
        @Index(name = "idx_org_score_organizer", columnList = "organizer_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false, unique = true)
    private Organizer organizer;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "total_events")
    @Builder.Default
    private int totalEvents = 0;

    @Column(name = "completed_events")
    @Builder.Default
    private int completedEvents = 0;

    @Column(name = "total_registrations")
    @Builder.Default
    private long totalRegistrations = 0;

    @Column(name = "total_attendance")
    @Builder.Default
    private long totalAttendance = 0;

    /** 0–100 attendance rate */
    @Column(name = "attendance_rate")
    @Builder.Default
    private Double attendanceRate = 0.0;

    /** 0–100 refund ratio (lower is better) */
    @Column(name = "refund_ratio")
    @Builder.Default
    private Double refundRatio = 0.0;

    /** 0–100 cancellation rate (lower is better) */
    @Column(name = "cancellation_rate")
    @Builder.Default
    private Double cancellationRate = 0.0;

    /** Overall score 0–100 */
    @Column(name = "overall_score")
    @Builder.Default
    private Double overallScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge", length = 30)
    @Builder.Default
    private OrganizerBadge badge = OrganizerBadge.NONE;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean featured = false;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrganizerBadge {
        NONE, BRONZE, SILVER, GOLD, FEATURED
    }
}
