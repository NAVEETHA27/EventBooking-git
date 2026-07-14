package com.eventbooking.repository;

import com.eventbooking.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);

    boolean existsByUserIdAndBadgeType(Long userId, String badgeType);

    @Query("SELECT SUM(a.xpAwarded) FROM UserAchievement a WHERE a.user.id = :userId")
    Integer sumXpByUserId(@Param("userId") Long userId);
}
