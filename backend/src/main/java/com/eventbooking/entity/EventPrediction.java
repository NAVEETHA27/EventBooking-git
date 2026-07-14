package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI predictions for an event: registrations, attendance, revenue, success score.
 */
@Entity
@Table(name = "event_predictions", indexes = {
        @Index(name = "idx_prediction_event", columnList = "event_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "predicted_registrations")
    private Integer predictedRegistrations;

    @Column(name = "predicted_attendance")
    private Integer predictedAttendance;

    @Column(name = "predicted_no_show_rate")
    private Double predictedNoShowRate;

    @Column(name = "predicted_revenue", precision = 12, scale = 2)
    private BigDecimal predictedRevenue;

    @Column(name = "predicted_food_count")
    private Integer predictedFoodCount;

    @Column(name = "predicted_certificate_count")
    private Integer predictedCertificateCount;

    /** 0–100 success score */
    @Column(name = "success_score")
    private Double successScore;

    @Column(name = "ai_suggestions", columnDefinition = "TEXT")
    private String aiSuggestions;

    @Column(name = "budget_estimate", precision = 12, scale = 2)
    private BigDecimal budgetEstimate;

    @Column(name = "expected_revenue", precision = 12, scale = 2)
    private BigDecimal expectedRevenue;

    @Column(name = "break_even_point", precision = 12, scale = 2)
    private BigDecimal breakEvenPoint;

    @Column(name = "estimated_profit", precision = 12, scale = 2)
    private BigDecimal estimatedProfit;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
