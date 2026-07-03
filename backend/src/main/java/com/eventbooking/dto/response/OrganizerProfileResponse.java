package com.eventbooking.dto.response;

import com.eventbooking.entity.Organizer;
import lombok.*;

/** Safe DTO — never exposes passwordHash or lazy collections. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizerProfileResponse {
    private Long    id;
    private String  organizerCode;
    private String  organizerName;
    private String  organizationName;
    private String  email;
    private String  phone;
    private String  address;
    private String  pinCode;
    private String  city;
    private String  state;
    private String  country;
    private String  website;
    private String  description;
    private String  organizationLogo;
    private boolean emailVerified;
    private String  role;

    public static OrganizerProfileResponse from(Organizer o) {
        return OrganizerProfileResponse.builder()
                .id(o.getId()).organizerCode(o.getOrganizerCode()).organizerName(o.getOrganizerName())
                .organizationName(o.getOrganizationName()).email(o.getEmail())
                .phone(o.getPhone()).address(o.getAddress()).pinCode(o.getPinCode())
                .city(o.getCity()).state(o.getState()).country(o.getCountry()).website(o.getWebsite())
                .description(o.getDescription()).organizationLogo(o.getOrganizationLogo())
                .emailVerified(o.isEmailVerified()).role(o.getRole().name())
                .build();
    }
}
