package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * XP / Level tracking for gamification.
 */
@Entity
@Table(name = "user_levels", indexes = {
        @Index(name = "idx_user_level_user", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp")
    @Builder.Default
    private int totalXp = 0;

    @Column(name = "current_level")
    @Builder.Default
    private int currentLevel = 1;

    @Column(name = "events_attended")
    @Builder.Default
    private int eventsAttended = 0;

    @Column(name = "events_registered")
    @Builder.Default
    private int eventsRegistered = 0;

    @Column(name = "certificates_earned")
    @Builder.Default
    private int certificatesEarned = 0;

    @Column(name = "reviews_given")
    @Builder.Default
    private int reviewsGiven = 0;

    @Column(name = "total_hours_completed")
    @Builder.Default
    private int totalHoursCompleted = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
