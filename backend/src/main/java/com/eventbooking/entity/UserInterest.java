package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User preference profile used by recommendation engine:
 * skills, interests, department, favorite categories, etc.
 */
@Entity
@Table(name = "user_interests", indexes = {
        @Index(name = "idx_user_interest_user", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Comma-separated skill tags */
    @Column(name = "skills", length = 500)
    private String skills;

    /** Comma-separated interest categories */
    @Column(name = "interests", length = 500)
    private String interests;

    @Column(name = "department", length = 150)
    private String department;

    @Column(name = "college", length = 200)
    private String college;

    @Column(name = "year_of_study", length = 20)
    private String yearOfStudy;

    /** Comma-separated favorite event categories */
    @Column(name = "favorite_categories", length = 300)
    private String favoriteCategories;

    @Column(name = "preferred_event_type", length = 20)
    private String preferredEventType;

    @Column(name = "career_goal", length = 300)
    private String careerGoal;

    // ── Extended AI fields (nullable — backward compatible) ───────────────

    /** Max budget for paid events in INR — null = no preference */
    @Column(name = "max_budget")
    private Integer maxBudget;

    /** FREE / PAID / BOTH — null = no preference */
    @Column(name = "paid_preference", length = 10)
    private String paidPreference;

    /** BEGINNER / INTERMEDIATE / ADVANCED */
    @Column(name = "skill_level", length = 20)
    private String skillLevel;

    /** Comma-separated exam months e.g. "APRIL,NOVEMBER" — avoid recommending during exams */
    @Column(name = "exam_months", length = 100)
    private String examMonths;

    /** Comma-separated preferred days e.g. "SATURDAY,SUNDAY" */
    @Column(name = "preferred_days", length = 100)
    private String preferredDays;

    /** AI-generated behavioral summary updated by the learning engine */
    @Column(name = "behavioral_summary", columnDefinition = "TEXT")
    private String behavioralSummary;

    /** Total click events tracked by behavioral engine */
    @Column(name = "total_clicks")
    @Builder.Default
    private Integer totalClicks = 0;

    /** Total searches recorded */
    @Column(name = "total_searches")
    @Builder.Default
    private Integer totalSearches = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
