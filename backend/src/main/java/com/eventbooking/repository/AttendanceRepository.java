package com.eventbooking.repository;

import com.eventbooking.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByTicketId(String ticketId);

    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.booking.user.id = :userId AND a.booking.event.id = :eventId")
    boolean existsByBookingUserIdAndBookingEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("SELECT a FROM Attendance a WHERE a.booking.event.id = :eventId")
    List<Attendance> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.booking.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);
}
