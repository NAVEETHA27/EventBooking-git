package com.eventbooking.repository;

import com.eventbooking.entity.EventRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRecommendationRepository extends JpaRepository<EventRecommendation, Long> {

    @Query("SELECT r FROM EventRecommendation r WHERE r.user.id = :userId AND r.recommendationCategory = :category " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) ORDER BY r.rankPosition ASC")
    List<EventRecommendation> findByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("now") LocalDateTime now);

    @Query("SELECT r FROM EventRecommendation r WHERE r.user.id = :userId " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) ORDER BY r.rankPosition ASC")
    List<EventRecommendation> findAllActiveForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EventRecommendation r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EventRecommendation r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
