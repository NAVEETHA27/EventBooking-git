package com.eventbooking.mapper;

import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.entity.Event;
import org.springframework.stereotype.Component;

/**
 * Maps between Event entity and EventResponse DTO.
 * Centralises mapping logic that was previously duplicated inside EventService.
 */
@Component
public class EventMapper {

    public EventResponse toResponse(Event e) {
        if (e == null) return null;
        return EventResponse.builder()
                .id(e.getId())
                .eventName(e.getEventName())
                .description(e.getDescription())
                .category(e.getCategory())
                .eventType(e.getEventType())
                .collegeName(e.getCollegeName())
                .departmentName(e.getDepartmentName())
                .eventBanner(e.getEventBanner())
                .eventDate(e.getEventDate())
                .eventTime(e.getEventTime())
                .endDate(e.getEndDate())
                .endTime(e.getEndTime())
                .registrationDeadline(e.getRegistrationDeadline())
                .venueName(e.getVenueName())
                .location(e.getLocation())
                .googleMapsUrl(e.getGoogleMapsUrl())
                .ticketPrice(e.getTicketPrice())
                .totalSeats(e.getTotalSeats())
                .availableSeats(e.getAvailableSeats())
                .status(e.getStatus())
                .visibility(e.getVisibility())
                .featured(e.isFeatured())
                .hasCertificate(e.isHasCertificate())
                .tags(e.getTags())
                .organizerDetails(e.getOrganizerDetails())
                .authorizedDocumentUrl(e.getAuthorizedDocumentUrl())
                .createdAt(e.getCreatedAt())
                .organizer(e.getOrganizer() != null
                        ? EventResponse.OrganizerInfo.builder()
                                .id(e.getOrganizer().getId())
                                .organizerName(e.getOrganizer().getOrganizerName())
                                .organizationName(e.getOrganizer().getOrganizationName())
                                .organizationLogo(e.getOrganizer().getOrganizationLogo())
                                .build()
                        : null)
                .build();
    }
}
