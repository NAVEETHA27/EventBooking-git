package com.eventbooking.dto.response;

import com.eventbooking.entity.Event;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventResponse {
    private Long id;
    private String eventName;
    private String description;
    private String category;
    private String eventType;
    private String collegeName;
    private String departmentName;
    private String eventBanner;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private LocalDate registrationDeadline;
    private String venueName;
    private String location;
    private String googleMapsUrl;
    private BigDecimal ticketPrice;
    private int totalSeats;
    private int availableSeats;
    private Event.EventStatus status;
    private Event.EventVisibility visibility;
    private boolean featured;
    private boolean hasCertificate;
    private String tags;
    private String organizerDetails;
    private String authorizedDocumentUrl;
    private OrganizerInfo organizer;
    private LocalDateTime createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrganizerInfo {
        private Long id;
        private String organizerName;
        private String organizationName;
        private String organizationLogo;
    }
}
