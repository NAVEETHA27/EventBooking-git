package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentToolRequest {
    @NotBlank
    private String prompt;

    private String toolName;

    @Builder.Default
    private Map<String, Object> arguments = new HashMap<>();
}
