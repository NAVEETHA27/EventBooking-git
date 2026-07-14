package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * College leaderboard entry — computed from participation, ratings, certificates.
 */
@Entity
@Table(name = "college_rankings", indexes = {
        @Index(name = "idx_college_name", columnList = "college_name", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_name", nullable = false, unique = true, length = 200)
    private String collegeName;

    @Column(name = "total_participants")
    @Builder.Default
    private long totalParticipants = 0;

    @Column(name = "total_events_attended")
    @Builder.Default
    private long totalEventsAttended = 0;

    @Column(name = "total_certificates")
    @Builder.Default
    private long totalCertificates = 0;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "overall_score")
    @Builder.Default
    private Double overallScore = 0.0;

    @Column(name = "rank_position")
    private int rankPosition;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
