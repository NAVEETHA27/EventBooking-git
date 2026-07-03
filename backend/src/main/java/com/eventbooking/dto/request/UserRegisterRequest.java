package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8–64 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number")
    private String phone;

    private String address;
    private String street;
    private String area;
    private String pinCode;
    private String organizationName;
    private String city;
    private String district;
    private String state;
    private String country;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private LocalDate dateOfBirth;
    private String gender;
}
