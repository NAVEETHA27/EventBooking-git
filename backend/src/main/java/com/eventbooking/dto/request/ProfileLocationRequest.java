package com.eventbooking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileLocationRequest {
    @Size(max = 300)
    private String address;
    @Size(max = 120)
    private String street;
    @Size(max = 120)
    private String area;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String district;
    @Size(max = 100)
    private String state;
    @Size(max = 100)
    private String country;
    @Size(max = 20)
    private String pinCode;
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal longitude;
}
