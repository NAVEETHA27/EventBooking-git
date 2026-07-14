package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI sentiment analysis result for an event's aggregate reviews.
 */
@Entity
@Table(name = "sentiment_analyses", indexes = {
        @Index(name = "idx_sentiment_event", columnList = "event_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SentimentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "positive_count")
    @Builder.Default
    private int positiveCount = 0;

    @Column(name = "neutral_count")
    @Builder.Default
    private int neutralCount = 0;

    @Column(name = "negative_count")
    @Builder.Default
    private int negativeCount = 0;

    /** 0–10 overall satisfaction score */
    @Column(name = "satisfaction_score")
    @Builder.Default
    private Double satisfactionScore = 0.0;

    /** JSON array of strength strings */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /** JSON array of weakness strings */
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    /** JSON array of improvement suggestion strings */
    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    /** AI-generated improvement report text */
    @Column(name = "improvement_report", columnDefinition = "TEXT")
    private String improvementReport;

    /** AI-generated testimonial text from best reviews */
    @Column(name = "ai_testimonial", columnDefinition = "TEXT")
    private String aiTestimonial;

    @Column(name = "total_reviews_analyzed")
    @Builder.Default
    private int totalReviewsAnalyzed = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
