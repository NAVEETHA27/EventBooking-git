package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByTicketId(String ticketId);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
          AND b.event.id = :eventId
          AND b.bookingStatus IN ('CONFIRMED', 'PENDING')
    """)
    Optional<Booking> findActiveBooking(@Param("userId") Long userId,
                                        @Param("eventId") Long eventId);

    @Query("""
        SELECT SUM(b.quantity) FROM Booking b
        WHERE b.event.id = :eventId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Integer sumConfirmedQuantity(@Param("eventId") Long eventId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.event e
        JOIN FETCH b.user u
        WHERE e.organizer.id = :organizerId
    """)
    Page<Booking> findByOrganizerEvents(@Param("organizerId") Long organizerId, Pageable pageable);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.participants
        JOIN FETCH b.user
        JOIN FETCH b.event e
        JOIN FETCH e.organizer
        WHERE e.id = :eventId
          AND e.organizer.id = :organizerId
          AND b.bookingStatus = 'CONFIRMED'
        ORDER BY b.bookedAt DESC
    """)
    List<Booking> findConfirmedByEventIdAndOrganizerId(@Param("eventId") Long eventId,
                                                       @Param("organizerId") Long organizerId);

    long countByEventIdAndBookingStatus(Long eventId, Booking.BookingStatus bookingStatus);

    long countByEventIdAndBookingStatusAndAttendanceStatus(Long eventId,
                                                           Booking.BookingStatus bookingStatus,
                                                           Booking.AttendanceStatus attendanceStatus);

    List<Booking> findByBookingStatusAndBookedAtBefore(Booking.BookingStatus status, LocalDateTime bookedAt);

    @Modifying
    @Query("""
        UPDATE Booking b
        SET b.ticketStatus = :expired,
            b.expiredAt = :expiredAt
        WHERE b.event.id = :eventId
          AND b.bookingStatus = :confirmed
          AND b.ticketStatus = :active
    """)
    int expireActiveTicketsForEvent(@Param("eventId") Long eventId,
                                    @Param("confirmed") Booking.BookingStatus confirmed,
                                    @Param("active") Booking.TicketStatus active,
                                    @Param("expired") Booking.TicketStatus expired,
                                    @Param("expiredAt") LocalDateTime expiredAt);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.event.id = :eventId AND b.bookingStatus = 'CONFIRMED'")
    Optional<Booking> findConfirmedByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.participants
        JOIN FETCH b.user
        WHERE b.event.id = :eventId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    List<Booking> findConfirmedWithDetailsByEventId(@Param("eventId") Long eventId);

    /** For certificate backfill — confirmed bookings across multiple events */
    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.event.id IN :eventIds AND b.bookingStatus = 'CONFIRMED'")
    List<Booking> findConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    /** College participation leaderboard: college name + participant count */
    @Query("SELECT p.college, COUNT(p.id) FROM Participant p WHERE p.college IS NOT NULL GROUP BY p.college ORDER BY COUNT(p.id) DESC")
    List<Object[]> findCollegeParticipation(Pageable pageable);
}
