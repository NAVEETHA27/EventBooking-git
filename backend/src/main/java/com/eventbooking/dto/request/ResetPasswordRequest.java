package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String newPassword;

    /** USER or ORGANIZER */
    @NotBlank(message = "Role is required")
    private String role;
}
