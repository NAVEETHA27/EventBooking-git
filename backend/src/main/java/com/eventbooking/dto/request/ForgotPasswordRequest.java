package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** USER or ORGANIZER */
    @NotBlank(message = "Role is required")
    private String role;
}
