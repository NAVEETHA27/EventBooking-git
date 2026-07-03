package com.eventbooking.repository;

import com.eventbooking.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByTicketId(String ticketId);
}
