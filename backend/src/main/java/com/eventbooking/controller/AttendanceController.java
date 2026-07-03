package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.exception.BookingException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.entity.Attendance;
import com.eventbooking.entity.Booking;
import com.eventbooking.repository.AttendanceRepository;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceRepository attendanceRepository;
    private final BookingRepository bookingRepository;
    private final AuditService auditService;

    @PostMapping("/scan/{ticketId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Transactional
    public ApiResponse<Attendance> scan(@PathVariable String ticketId, @AuthenticationPrincipal AuthPrincipal principal) {
        if (attendanceRepository.findByTicketId(ticketId).isPresent()) {
            throw new BookingException("Ticket already checked in");
        }
        Booking booking = bookingRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (!booking.getEvent().getOrganizer().getId().equals(principal.getId())) {
            throw new BookingException("Ticket does not belong to your event");
        }
        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new BookingException("Only confirmed tickets can be checked in");
        }
        Attendance attendance = Attendance.builder()
                .ticketId(ticketId)
                .booking(booking)
                .checkedInAt(LocalDateTime.now())
                .scannedByOrganizerId(principal.getId())
                .build();
        Attendance saved = attendanceRepository.save(attendance);
        auditService.record(principal.getId(), "ORGANIZER", "TICKET_CHECK_IN", "ATTENDANCE",
                String.valueOf(saved.getId()), "Ticket " + ticketId + " checked in");
        return ApiResponse.success("Check-in successful", saved);
    }
}
