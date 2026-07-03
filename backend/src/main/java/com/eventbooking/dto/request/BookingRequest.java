package com.eventbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Maximum 10 tickets per booking")
    private int quantity;

    @Builder.Default
    private List<ParticipantRequest> participants = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ParticipantRequest {
        @NotBlank(message = "Participant name is required")
        private String name;

        @NotBlank(message = "Participant email is required")
        @Email(message = "Invalid participant email")
        private String email;

        private String department;
        private String college;
    }
}
