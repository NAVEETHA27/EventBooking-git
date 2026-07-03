package com.eventbooking.dto.response;

import com.eventbooking.entity.User;
import lombok.*;
import java.time.LocalDate;

/** Safe DTO — never exposes passwordHash or lazy collections. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private Long    id;
    private String  userCode;
    private String  name;
    private String  email;
    private String  phone;
    private String  address;
    private String  pinCode;
    private String  organizationName;
    private String  city;
    private String  state;
    private String  country;
    private String  gender;
    private LocalDate dateOfBirth;
    private String  profilePicture;
    private boolean emailVerified;
    private String  role;

    public static UserProfileResponse from(User u) {
        return UserProfileResponse.builder()
                .id(u.getId()).userCode(u.getUserCode()).name(u.getName()).email(u.getEmail())
                .phone(u.getPhone()).address(u.getAddress()).pinCode(u.getPinCode())
                .organizationName(u.getOrganizationName()).city(u.getCity()).state(u.getState()).country(u.getCountry())
                .gender(u.getGender())
                .dateOfBirth(u.getDateOfBirth()).profilePicture(u.getProfilePicture())
                .emailVerified(u.isEmailVerified()).role(u.getRole().name())
                .build();
    }
}
