package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "profile_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileLocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "organizer_id")
    private Long organizerId;
    private String address;
    private String street;
    private String area;
    private String city;
    private String district;
    private String state;
    private String country;
    @Column(name = "pin_code", length = 20)
    private String pinCode;
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
