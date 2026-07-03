package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutorialVideoRequest {
    @NotBlank @Size(max = 160)
    private String title;
    private String description;
    @NotBlank
    private String videoUrl;
    private String category;
    private boolean active = true;
}
