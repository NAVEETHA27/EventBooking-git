package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    /** Fetch event with organizer eagerly loaded — used by scheduler transitions. */
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.id = :id")
    Optional<Event> findByIdWithOrganizer(@Param("id") Long id);

    long countByTagsContaining(String tag);

    boolean existsByEventName(String eventName);

    @Transactional
    void deleteByTagsContaining(String tag);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Event e
        SET e.wheelchairAccessible = false,
            e.restRoomsAvailable = false,
            e.drinkingWaterAvailable = false,
            e.networkingEnabled = false
    """)
    int clearRemovedFacilityFlags();

    @Modifying
    @Transactional
    @Query("""
        UPDATE Event e SET e.availableSeats =
            CASE WHEN e.availableSeats + :qty > e.totalSeats THEN e.totalSeats
                 ELSE e.availableSeats + :qty END
        WHERE e.id = :id
    """)
    void incrementAvailableSeats(@Param("id") Long id, @Param("qty") int qty);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // Spring Data derives this correctly from organizer.id
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    long countByOrganizerId(Long organizerId);

    List<Event> findByStatus(Event.EventStatus status);

    List<Event> findByStatusIn(List<Event.EventStatus> statuses);

    // Use enum parameters instead of string literals in JPQL
    @Query("SELECT e FROM Event e WHERE e.status IN (:statuses) AND e.visibility = :pub AND e.eventDate >= :today ORDER BY e.eventDate ASC")
    List<Event> findUpcomingPublicEvents(
            @Param("today")    LocalDate today,
            @Param("statuses") List<Event.EventStatus> statuses,
            @Param("pub")      Event.EventVisibility pub);

    default List<Event> findUpcomingPublicEvents(LocalDate today) {
        return findUpcomingPublicEvents(today,
                List.of(Event.EventStatus.PUBLISHED,
                        Event.EventStatus.LIVE,
                        Event.EventStatus.ONGOING,
                        Event.EventStatus.UPCOMING),
                Event.EventVisibility.PUBLIC);
    }

    @Query("""
        SELECT e FROM Event e
        WHERE e.organizer.id = :organizerId
          AND (:status IS NULL OR e.status = :status)
    """)
    Page<Event> findByOrganizerIdAndStatus(
            @Param("organizerId") Long organizerId,
            @Param("status")      Event.EventStatus status,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Event e SET e.status = :completed
        WHERE e.eventDate < :today
          AND e.status IN :statuses
    """)
    int markPastEventsCompleted(
            @Param("today")     LocalDate today,
            @Param("completed") Event.EventStatus completed,
            @Param("statuses")  List<Event.EventStatus> statuses);

    default int markPastEventsCompleted(LocalDate today) {
        return markPastEventsCompleted(
                today,
                Event.EventStatus.COMPLETED,
                List.of(Event.EventStatus.PUBLISHED,
                        Event.EventStatus.UPCOMING,
                        Event.EventStatus.ONGOING));
    }

    /** Used by EventScheduler safety net — find events stuck in these statuses before the cutoff. */
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.createdAt <= :cutoff")
    List<Event> findByStatusInAndCreatedAtBefore(
            @Param("statuses") List<Event.EventStatus> statuses,
            @Param("cutoff")   java.time.LocalDateTime cutoff);

    /** Find all PUBLISHED events whose eventDate is today or in the future (for UPCOMING → LIVE transition). */
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.eventDate <= :today")
    List<Event> findByStatusInAndEventDateLessThanEqual(
            @Param("statuses") List<Event.EventStatus> statuses,
            @Param("today")    LocalDate today);

    /** Find LIVE/UPCOMING events whose end date has passed → COMPLETED. */
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.endDate IS NOT NULL AND e.endDate < :today")
    List<Event> findByStatusInAndEndDateBefore(
            @Param("statuses") List<Event.EventStatus> statuses,
            @Param("today")    LocalDate today);

    /** Find COMPLETED events whose certificate deadline has passed → EXPIRED. */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.certificateDeadline IS NOT NULL AND e.certificateDeadline < :today")
    List<Event> findCompletedPastCertDeadline(
            @Param("today")  LocalDate today,
            @Param("status") Event.EventStatus status);

    default List<Event> findCompletedPastCertDeadline(LocalDate today) {
        return findCompletedPastCertDeadline(today, Event.EventStatus.COMPLETED);
    }

    /** Find PUBLISHED/UPCOMING events whose registrationDeadline has passed → EXPIRED. */
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.registrationDeadline IS NOT NULL AND e.registrationDeadline < :today")
    List<Event> findPublishedPastRegistrationDeadline(
            @Param("today")    LocalDate today,
            @Param("statuses") List<Event.EventStatus> statuses);

    default List<Event> findPublishedPastRegistrationDeadline(LocalDate today) {
        return findPublishedPastRegistrationDeadline(today,
                List.of(Event.EventStatus.PUBLISHED, Event.EventStatus.UPCOMING));
    }

    /** Find PUBLISHED events with no endDate whose eventDate has passed → COMPLETED. */
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.endDate IS NULL AND e.eventDate < :today")
    List<Event> findByStatusInAndEventDateBeforeAndEndDateIsNull(
            @Param("statuses") List<Event.EventStatus> statuses,
            @Param("today")    LocalDate today);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.event.id = :eventId
          AND b.bookingStatus = :status
    """)
    long countConfirmedBookings(
            @Param("eventId") Long eventId,
            @Param("status")  Booking.BookingStatus status);

    default long countConfirmedBookings(Long eventId) {
        return countConfirmedBookings(eventId, Booking.BookingStatus.CONFIRMED);
    }
}
