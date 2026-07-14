package com.eventbooking.repository;

import com.eventbooking.entity.EventRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRatingRepository extends JpaRepository<EventRating, Long> {

    Optional<EventRating> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Page<EventRating> findByEventIdAndModerationStatusOrderByCreatedAtDesc(
            Long eventId, EventRating.ModerationStatus status, Pageable pageable);

    List<EventRating> findByEventIdAndModerationStatus(Long eventId, EventRating.ModerationStatus status);

    @Query("SELECT AVG(r.overallRating) FROM EventRating r WHERE r.event.id = :eventId AND r.moderationStatus = :status")
    Optional<Double> findAverageRatingByEventId(@Param("eventId") Long eventId,
            @Param("status") EventRating.ModerationStatus status);

    default Optional<Double> findAverageRatingByEventId(Long eventId) {
        return findAverageRatingByEventId(eventId, EventRating.ModerationStatus.APPROVED);
    }

    @Query("SELECT COUNT(r) FROM EventRating r WHERE r.event.id = :eventId AND r.moderationStatus = :status")
    long countApprovedByEventId(@Param("eventId") Long eventId,
            @Param("status") EventRating.ModerationStatus status);

    default long countApprovedByEventId(Long eventId) {
        return countApprovedByEventId(eventId, EventRating.ModerationStatus.APPROVED);
    }

    @Query("SELECT r FROM EventRating r WHERE r.event.id = :eventId AND r.verifiedAttendance = true ORDER BY r.overallRating DESC")
    List<EventRating> findTopRatingsByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT r FROM EventRating r WHERE r.fakeFlagged = false AND r.moderationStatus = 'PENDING'")
    Page<EventRating> findPendingModeration(Pageable pageable);

    @Query("SELECT r FROM EventRating r WHERE r.fakeFlagged = true")
    Page<EventRating> findFlagged(Pageable pageable);

    List<EventRating> findByUserId(Long userId);

    @Query("SELECT r FROM EventRating r WHERE r.event.organizer.id = :organizerId AND r.moderationStatus = 'APPROVED'")
    List<EventRating> findByOrganizerId(@Param("organizerId") Long organizerId);
}
