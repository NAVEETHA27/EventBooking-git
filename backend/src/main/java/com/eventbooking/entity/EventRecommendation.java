package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Personalized event recommendation for a user (cached result of AI engine).
 */
@Entity
@Table(name = "event_recommendations", indexes = {
        @Index(name = "idx_rec_user", columnList = "user_id"),
        @Index(name = "idx_rec_event", columnList = "event_id"),
        @Index(name = "idx_rec_category", columnList = "recommendation_category")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** e.g. RECOMMENDED_FOR_YOU, AI_PICKS, TRENDING, NEAR_YOU, POPULAR, etc. */
    @Column(name = "recommendation_category", length = 50)
    private String recommendationCategory;

    /** 0–100 match score */
    @Column(name = "match_score")
    @Builder.Default
    private double matchScore = 0.0;

    /** Human-readable reason from AI */
    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "rank_position")
    private int rankPosition;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Cache expires after this time; null means no expiry */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
