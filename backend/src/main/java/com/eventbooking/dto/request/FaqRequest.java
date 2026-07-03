package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqRequest {
    @NotBlank @Size(max = 120)
    private String category;
    @NotBlank @Size(max = 240)
    private String question;
    @NotBlank
    private String answer;
    private boolean active = true;
}
