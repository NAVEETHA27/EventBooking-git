package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Gamification achievements and XP tracking for a user.
 */
@Entity
@Table(name = "user_achievements", indexes = {
        @Index(name = "idx_achievement_user", columnList = "user_id"),
        @Index(name = "idx_achievement_badge", columnList = "badge_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_type", nullable = false, length = 50)
    private String badgeType;

    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;

    @Column(name = "badge_description", length = 300)
    private String badgeDescription;

    @Column(name = "xp_awarded")
    @Builder.Default
    private int xpAwarded = 0;

    @Column(name = "icon_url", length = 300)
    private String iconUrl;

    @CreatedDate
    @Column(name = "earned_at", updatable = false)
    private LocalDateTime earnedAt;
}
