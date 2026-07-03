package com.eventbooking.mapper;

import com.eventbooking.dto.response.OrganizerProfileResponse;
import com.eventbooking.entity.Organizer;
import org.springframework.stereotype.Component;

/**
 * Maps between Organizer entity and OrganizerProfileResponse DTO.
 */
@Component
public class OrganizerMapper {

    public OrganizerProfileResponse toResponse(Organizer organizer) {
        if (organizer == null) return null;
        return OrganizerProfileResponse.builder()
                .id(organizer.getId())
                .organizerCode(organizer.getOrganizerCode())
                .organizerName(organizer.getOrganizerName())
                .organizationName(organizer.getOrganizationName())
                .email(organizer.getEmail())
                .phone(organizer.getPhone())
                .address(organizer.getAddress())
                .city(organizer.getCity())
                .state(organizer.getState())
                .country(organizer.getCountry())
                .pinCode(organizer.getPinCode())
                .website(organizer.getWebsite())
                .description(organizer.getDescription())
                .organizationLogo(organizer.getOrganizationLogo())
                .emailVerified(organizer.isEmailVerified())
                .role(organizer.getRole() != null ? organizer.getRole().name() : null)
                .build();
    }
}
