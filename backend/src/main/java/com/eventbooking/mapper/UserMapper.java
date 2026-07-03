package com.eventbooking.mapper;

import com.eventbooking.dto.response.UserProfileResponse;
import com.eventbooking.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps between User entity and UserProfileResponse DTO.
 */
@Component
public class UserMapper {

    public UserProfileResponse toResponse(User user) {
        if (user == null) return null;
        return UserProfileResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .pinCode(user.getPinCode())
                .organizationName(user.getOrganizationName())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .profilePicture(user.getProfilePicture())
                .emailVerified(user.isEmailVerified())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
