package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalDecisionRequest {
    @NotBlank
    private String decision;
    private String reason;
}
