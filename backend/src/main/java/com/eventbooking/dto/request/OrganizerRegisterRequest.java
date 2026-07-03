package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizerRegisterRequest {

    @NotBlank(message = "Organizer name is required")
    @Size(min = 2, max = 120)
    private String organizerName;

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 160)
    private String organizationName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number")
    private String phone;

    private String address;
    private String pinCode;
    private String city;
    private String state;
    private String country;
    private String website;
    private String description;
}
