package com.eventbooking.service;

import com.eventbooking.entity.*;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Gamification: XP, levels, badges, and achievements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserLevelRepository levelRepository;
    private final UserAchievementRepository achievementRepository;
    private final UserRepository userRepository;

    // XP rewards
    private static final int XP_REGISTRATION   = 10;
    private static final int XP_ATTENDANCE      = 25;
    private static final int XP_CERTIFICATE     = 50;
    private static final int XP_REVIEW          = 15;
    private static final int XP_FIRST_BOOKING   = 20;
    private static final int XP_EARLY_BIRD      = 30;

    // Badge definitions
    public static final Map<String, String[]> BADGES = Map.of(
            "EVENT_EXPLORER",       new String[]{"Event Explorer", "Attended 3+ events", "🎪"},
            "TECH_ENTHUSIAST",      new String[]{"Tech Enthusiast", "Attended 3 tech events", "💻"},
            "AI_LEARNER",           new String[]{"AI Learner", "Attended an AI event", "🤖"},
            "TOP_REVIEWER",         new String[]{"Top Reviewer", "Submitted 5+ reviews", "⭐"},
            "EARLY_BIRD",           new String[]{"Early Bird", "Registered within 24h of event listing", "🐦"},
            "NETWORKING_PRO",       new String[]{"Networking Pro", "Connected with 5+ attendees", "🤝"},
            "CERTIFICATE_COLLECTOR",new String[]{"Certificate Collector", "Earned 3+ certificates", "🏆"},
            "FIRST_STEP",           new String[]{"First Step", "Booked your first event", "🎫"}
    );

    @Transactional
    public UserLevel getOrCreateLevel(Long userId) {
        return levelRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            return levelRepository.save(UserLevel.builder().user(user).build());
        });
    }

    @Transactional
    public void awardBookingXp(Long userId, boolean isFirst) {
        UserLevel level = getOrCreateLevel(userId);
        addXp(level, XP_REGISTRATION + (isFirst ? XP_FIRST_BOOKING : 0));
        level.setEventsRegistered(level.getEventsRegistered() + 1);
        levelRepository.save(level);

        if (isFirst) awardBadge(userId, "FIRST_STEP");
        checkProgressBadges(userId, level);
    }

    @Transactional
    public void awardAttendanceXp(Long userId) {
        UserLevel level = getOrCreateLevel(userId);
        addXp(level, XP_ATTENDANCE);
        level.setEventsAttended(level.getEventsAttended() + 1);
        levelRepository.save(level);
        checkProgressBadges(userId, level);
    }

    @Transactional
    public void awardCertificateXp(Long userId) {
        UserLevel level = getOrCreateLevel(userId);
        addXp(level, XP_CERTIFICATE);
        level.setCertificatesEarned(level.getCertificatesEarned() + 1);
        levelRepository.save(level);
        if (level.getCertificatesEarned() >= 3) awardBadge(userId, "CERTIFICATE_COLLECTOR");
    }

    @Transactional
    public void awardReviewXp(Long userId) {
        UserLevel level = getOrCreateLevel(userId);
        addXp(level, XP_REVIEW);
        level.setReviewsGiven(level.getReviewsGiven() + 1);
        levelRepository.save(level);
        if (level.getReviewsGiven() >= 5) awardBadge(userId, "TOP_REVIEWER");
    }

    @Transactional
    public void awardBadge(Long userId, String badgeType) {
        if (achievementRepository.existsByUserIdAndBadgeType(userId, badgeType)) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        String[] def = BADGES.get(badgeType);
        if (def == null) return;
        UserAchievement badge = UserAchievement.builder()
                .user(user)
                .badgeType(badgeType)
                .badgeName(def[0])
                .badgeDescription(def[1])
                .iconUrl(def[2])
                .xpAwarded(20)
                .build();
        achievementRepository.save(badge);
        log.info("Badge awarded: {} → user {}", badgeType, userId);
    }

    private void checkProgressBadges(Long userId, UserLevel level) {
        if (level.getEventsAttended() >= 3) awardBadge(userId, "EVENT_EXPLORER");
    }

    private void addXp(UserLevel level, int xp) {
        level.setTotalXp(level.getTotalXp() + xp);
        level.setCurrentLevel(computeLevel(level.getTotalXp()));
    }

    private int computeLevel(int totalXp) {
        // Level thresholds: 1=0, 2=100, 3=250, 4=500, 5=1000...
        if (totalXp >= 1000) return 5;
        if (totalXp >= 500)  return 4;
        if (totalXp >= 250)  return 3;
        if (totalXp >= 100)  return 2;
        return 1;
    }

    @Transactional
    public Map<String, Object> getGamificationProfile(Long userId) {
        UserLevel level = getOrCreateLevel(userId);
        List<UserAchievement> badges = achievementRepository.findByUserIdOrderByEarnedAtDesc(userId);
        return Map.of(
                "totalXp", level.getTotalXp(),
                "currentLevel", level.getCurrentLevel(),
                "eventsAttended", level.getEventsAttended(),
                "eventsRegistered", level.getEventsRegistered(),
                "certificatesEarned", level.getCertificatesEarned(),
                "reviewsGiven", level.getReviewsGiven(),
                "badges", badges,
                "nextLevelXp", nextLevelXp(level.getTotalXp()),
                "progressToNextLevel", progressPercent(level.getTotalXp())
        );
    }

    private int nextLevelXp(int current) {
        if (current < 100)  return 100 - current;
        if (current < 250)  return 250 - current;
        if (current < 500)  return 500 - current;
        if (current < 1000) return 1000 - current;
        return 0;
    }

    private int progressPercent(int current) {
        if (current >= 1000) return 100;
        int[] thresholds = {0, 100, 250, 500, 1000};
        for (int i = 1; i < thresholds.length; i++) {
            if (current < thresholds[i]) {
                int range = thresholds[i] - thresholds[i-1];
                int progress = current - thresholds[i-1];
                return (int) ((double) progress / range * 100);
            }
        }
        return 100;
    }
}
