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

    long countByTagsContaining(String tag);

    @Transactional
    void deleteByTagsContaining(String tag);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // Spring Data derives this correctly from organizer.id
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    List<Event> findByStatus(Event.EventStatus status);

    // Use enum parameters instead of string literals in JPQL
    @Query("SELECT e FROM Event e WHERE e.status = :published AND e.visibility = :pub AND e.eventDate >= :today ORDER BY e.eventDate ASC")
    List<Event> findUpcomingPublicEvents(
            @Param("today")     LocalDate today,
            @Param("published") Event.EventStatus published,
            @Param("pub")       Event.EventVisibility pub);

    default List<Event> findUpcomingPublicEvents(LocalDate today) {
        return findUpcomingPublicEvents(today,
                Event.EventStatus.PUBLISHED,
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
                Event.EventStatus.EXPIRED,
                List.of(Event.EventStatus.PUBLISHED,
                        Event.EventStatus.UPCOMING,
                        Event.EventStatus.ONGOING));
    }

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
