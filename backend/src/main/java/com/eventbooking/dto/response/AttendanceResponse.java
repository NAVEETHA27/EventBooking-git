package com.eventbooking.dto.response;

import com.eventbooking.entity.Booking;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceResponse {
    private Long bookingId;
    private String ticketId;
    private Long eventId;
    private String eventName;
    private Long userId;
    private String participantName;
    private String participantEmail;
    private Booking.AttendanceStatus attendanceStatus;
    private LocalDateTime checkInTime;
    private Long checkedInBy;
    private boolean certificateEligible;
    private String message;
}
