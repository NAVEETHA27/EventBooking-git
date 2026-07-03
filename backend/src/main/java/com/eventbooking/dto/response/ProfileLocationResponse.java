package com.eventbooking.dto.response;

import com.eventbooking.entity.ProfileLocation;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileLocationResponse {
    private Long id;
    private String address;
    private String street;
    private String area;
    private String city;
    private String district;
    private String state;
    private String country;
    private String pinCode;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static ProfileLocationResponse from(ProfileLocation location) {
        if (location == null) return null;
        return ProfileLocationResponse.builder()
                .id(location.getId())
                .address(location.getAddress())
                .street(location.getStreet())
                .area(location.getArea())
                .city(location.getCity())
                .district(location.getDistrict())
                .state(location.getState())
                .country(location.getCountry())
                .pinCode(location.getPinCode())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}
