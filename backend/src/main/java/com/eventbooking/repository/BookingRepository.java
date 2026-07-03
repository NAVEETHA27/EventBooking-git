package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Booking> findByBookingStatusAndBookedAtBefore(Booking.BookingStatus status, LocalDateTime bookedAt);
}
