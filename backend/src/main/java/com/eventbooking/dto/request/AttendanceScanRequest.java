package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceScanRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;
}
